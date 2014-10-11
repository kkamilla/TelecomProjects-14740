#!/usr/bin/python
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel


class NewTopo(Topo):
    " Creating the topology with 8 hosts and 6 switches."
    
    def __init__(self,n=3,**opts):
        #Initialize topology and default options
        Topo.__init__( self , **opts )
        switch1 = self.addSwitch( 's1' )
        host1=self.addHost('h1')
        self.addLink(host1,switch1)
        switch2 = self.addSwitch( 's2')
        
        linkopts12 = dict(bw=10, delay='1ms', loss=3)
        self.addLink(switch1,switch2, **linkopts12)
        switch3 = self.addSwitch( 's3')
        
        linkopts13 = dict(bw=15, delay='2ms', loss=2)
        self.addLink(switch1,switch3, **linkopts13)
        host2=self.addHost('h2')
        self.addLink(host2,switch2)
        host3=self.addHost('h3')
        self.addLink(host3,switch3)
        switch4 = self.addSwitch( 's4')
        
        linkopts24 = dict(bw=20, delay='4ms', loss=1)
        self.addLink(switch4,switch2, **linkopts24)
        switch5 = self.addSwitch( 's5')
        
        linkopts35 = dict(bw=20, delay='4ms', loss=1)
        self.addLink(switch3,switch5, **linkopts35)
        switch6 = self.addSwitch( 's6')
        
        linkopts56 = dict(bw=40, delay='10ms', loss=2)
        self.addLink(switch5,switch6, **linkopts56)
        host8=self.addHost('h8')
        self.addLink(host8,switch6)
        host4=self.addHost('h4')
        self.addLink(host4,switch4)
        host5=self.addHost('h5')
        self.addLink(host5,switch4)
        host6=self.addHost('h6')
        self.addLink(host6,switch5)
        host7=self.addHost('h7')
        self.addLink(host7,switch6)

def simpleTest():
    "Create and test the  network"
    topo = NewTopo( n=4 )
    net = Mininet( topo=topo, link=TCLink, host=CPULimitedHost )
    print "Starting the network"
    net.start()
    print "Dumping host connections"
    dumpNodeConnections(net.hosts)
    print "Testing network connectivity"
    hosts=net.hosts
    h1,h2,h3,h4,h5,h6,h7,h8=net.get('h1','h2','h3','h4','h5','h6','h7','h8');
    ping10(net, (h1,h2,h3,h4,h5,h6,h7,h8))
    net.stop()

def ping10(netname=None,hosts=None, timeout=None ):
    """Ping between all specified hosts.
        hosts: list of hosts
        timeout: time to wait for a response, as string
        returns: ploss packet loss percentage"""
    # should we check if running?
    packets = 0
    lost = 0
    ploss = None
    if not hosts:
        print( '*** Ping: testing ping reachability\n' )
    for node in hosts:
        print( '%s -> ' % node.name )
        for dest in hosts:
            if node != dest:
                opts = ''
                if timeout:
                    opts = '-W %s' % timeout
                result = node.cmd( 'ping -c10 %s %s' % (opts, dest.IP()) )
                sent, received = netname._parsePing( result )
                packets += sent
                if received > sent:
                    error( '*** Error: received too many packets' )
                    error( '%s' % result )
                    node.cmdPrint( 'route' )
                    exit( 1 )
                lost += sent - received
                print( ( '%s is reachable' % dest.name ) if received else 'X ' )
        ploss = 100 * lost / packets
        print( "*** Results: %i%% dropped (%d/%d lost)\n" %
              ( ploss, lost, packets ) )
    return ploss

topos = {'mytopo1':(lambda:NewTopo())}

if __name__ == '__main__':
    setLogLevel('info')
    simpleTest()
