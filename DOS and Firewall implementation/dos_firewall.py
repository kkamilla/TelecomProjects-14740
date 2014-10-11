#PROJECT 03  - Krutika Kamilla, Vidya Nambiar
"""
This program acts as a DoS defender and firewall.
Firewall functionality - All packets between hosts H1 and H2 are blocked
DoS Defender for Host H3 -  Monitor packets at host H1 and if the incoming packet count exceeds 2002, further packets will be blocked
"""
from pox.core import core
from pox.lib.addresses import EthAddr
from pox.lib.recoco import Timer
from pox.lib.revent import EventHalt
import pox.openflow.libopenflow_01 as of

mac1=EthAddr('00:00:00:00:00:01')   #Mac address of h1
mac2=EthAddr('00:00:00:00:00:02')   #Mac address of h2
mac3=EthAddr('00:00:00:00:00:03')   #Mac address of h3

log = core.getLogger("LOGGING...")  #logging

dos_detected = 0

class Defender (object):
  """
    A Defender object is created for the switch.
    A Connection object for that switch is passed to the __init__ function.
  """
  def __init__ (self, connection):
    self.connection = connection

    # Binding connection to PacketIn event listener
    connection.addListeners(self)

    # Table to keep track of ethernet address against switch port (key = MAC, value = port).
    self.mac_to_port = {}

  def resend_packet (self, packet_in, out_port):
    msg = of.ofp_packet_out()
    msg.data = packet_in

    # Action to send to the specified port
    action = of.ofp_action_output(port = out_port)
    msg.actions.append(action)

    # Sending a message to the switch
    self.connection.send(msg)

  def act_like_hub (self, packet, packet_in):
    """
    Send output to all ports except the input port
    """
    self.resend_packet(packet_in, of.OFPP_ALL)

  def act_like_switch (self, packet, packet_in):
    """
    Implement a learning switch -
    """
    source_macaddr=str(packet.src)
    dest_macaddr=str(packet.dst)
    self.mac_to_port[source_macaddr]=packet_in.in_port

    if dest_macaddr in self.mac_to_port:

      log.debug("MAC addr %s and port %s" % (source_macaddr,packet_in.in_port))
      log.debug("Installing flow...")

          #Initializing ofp_flow_mod message fields
      msg=of.ofp_flow_mod()
      msg.buffer_id=packet_in.buffer_id
      msg.match.dl_src=packet.src
      msg.match.dl_dst=packet.dst
      msg.idle_timeout=10
      msg.hard_timeout=20
      msg.actions.append(of.ofp_action_output(port=self.mac_to_port[dest_macaddr]))
      self.connection.send(msg)

    else:

      #Flood the packet out everything but the input port
      log.debug("Flood")
      self.resend_packet(packet_in, of.OFPP_ALL)

  def _handle_PacketIn (self, event):
    log.debug("event parsed %s" %(event.parsed.src))
    packet = event.parsed # This is the parsed packet data.
    if not packet.parsed:
      log.warning("Ignoring incomplete packet")
      return

    packet_in = event.ofp # The actual ofp_packet_in message.

    self.act_like_switch(packet, packet_in)

def _handle_flow_stats(event):
    log.debug("Measuring flow STATISTICS")
    bytes_counter=0
    flow_counter=0
    for f in event.stats:
        if f.match.dl_dst ==mac3:
            bytes_counter += f.byte_count
            flow_counter +=1
            log.debug("\nCounting statistics of Host H3")
                # Check for DoS
            if bytes_counter > 500:
                log.info("\n Traffic at Host H3: %s bytes over %s flows",bytes_counter,flow_counter)
                global dos_detected
                dos_detected = 1
                return EventHalt
    log.info("\n Traffic at Host H3: %s bytes over %s flows",bytes_counter,flow_counter)

def _timer_func ():
    for connection in core.openflow._connections.values():
       connection.send(of.ofp_stats_request(body=of.ofp_flow_stats_request()))
    log.debug(" Flow request %i sent",len(core.openflow._connections))

def _handle_dos(event):
    packet=event.parsed
    if packet.dst==mac3:
        if dos_detected==1:
            log.debug("Detected DoS attack...Blocking packets to host H3!!")
            return EventHalt

def _handle_PacketInFirewall(event):
    packet=event.parsed
    if packet.dst==mac2:
        if packet.src==mac1:
            log.debug("\n Blocked from H1 to H2 due to Firewall")
            return EventHalt
    if packet.dst==mac1:
        if packet.src==mac2:
            log.debug("\n Blocked from H2 to H1 due to Firewall")
            return EventHalt


def launch ():

  def start_switch (event):
    log.debug("Controlling %s" % (event.connection,))
    Defender(event.connection)

  core.openflow.addListenerByName("ConnectionUp", start_switch)
print("\n  MENU")
print("\n1. Firewall preventing packet flow between hosts H1 and H2")
print("\n2. Monitor statistics of Host H3 and prevent DoS")
choice=raw_input("\nEnter your choice: ")
choice=int(choice)
if choice==1:
    core.openflow.addListenerByName("PacketIn",_handle_PacketInFirewall)
elif choice==2:
    core.openflow.addListenerByName("PacketIn",_handle_dos)
    core.openflow.addListenerByName("FlowStatsReceived",_handle_flow_stats)
    Timer(1,_timer_func,recurring=True)

