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
    print "UDP Performance"
    h1,h2=net.get('h1','h2');
    h1,h3=net.get('h1','h3');
    h1,h4=net.get('h1','h4');
    h1,h5=net.get('h1','h5');
    h1,h6=net.get('h1','h6');
    h1,h7=net.get('h1','h7');
    h1,h8=net.get('h1','h8');
    h2,h3=net.get('h2','h3');
    h2,h4=net.get('h2','h4');
    h2,h5=net.get('h2','h5');
    h2,h6=net.get('h2','h6');
    h2,h7=net.get('h2','h7');
    h2,h8=net.get('h2','h8');
    h3,h4=net.get('h3','h4');
    h3,h5=net.get('h3','h5');
    h3,h6=net.get('h3','h6');
    h3,h7=net.get('h3','h7');
    h3,h8=net.get('h3','h8');
    h4,h5=net.get('h4','h5');
    h4,h6=net.get('h4','h6');
    h4,h7=net.get('h4','h7');
    h4,h8=net.get('h4','h8');
    h5,h6=net.get('h5','h6');
    h5,h7=net.get('h5','h7');
    h5,h8=net.get('h5','h8');
    h6,h7=net.get('h6','h7');
    h6,h8=net.get('h6','h8');
    h7,h8=net.get('h7','h8');
    perfopts=dict(l4Type='UDP',udpBw='15m')
    net.iperf((h1,h2),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h3),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h4),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h5),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h6),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h1,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h3),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h4),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h5),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h6),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h2,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h3,h4),l4Type='UDP',udpBw='15m')
    net.iperf((h3,h5),l4Type='UDP',udpBw='15m')
    net.iperf((h3,h6),l4Type='UDP',udpBw='15m')
    net.iperf((h3,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h3,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h4,h5),l4Type='UDP',udpBw='15m')
    net.iperf((h4,h6),l4Type='UDP',udpBw='15m')
    net.iperf((h4,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h4,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h5,h6),l4Type='UDP',udpBw='15m')
    net.iperf((h5,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h5,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h6,h7),l4Type='UDP',udpBw='15m')
    net.iperf((h6,h8),l4Type='UDP',udpBw='15m')
    net.iperf((h7,h8),l4Type='UDP',udpBw='15m')
    net.stop()

topos = {'mytopo1':(lambda:NewTopo())}

if __name__ == '__main__':
    setLogLevel('info')
    simpleTest()