package com.pastrytutorial.lesson4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Vector;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class DistTutorial {
    // this will keep track of our applications
    Vector<MyApp> apps = new Vector<MyApp>();

    /**
     * This constructor launches numNodes PastryNodes.  They will bootstrap
     * to an existing ring if one exists at the specified location, otherwise
     * it will start a new ring.
     *
     * @param bindport the local port to bind to
     * @param bootaddress the IP:port of the node to boot from
     * @param numNodes the number of nodes to create in this JVM
     * @param env the environment for these nodes
     */
    public DistTutorial(int bindport, InetSocketAddress bootaddress, int numNodes, Environment env) throws Exception {

        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

        // construct the PastryNodeFactory, this is how we use rice.pastry.socket
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

        // loop to construct the nodes/apps
        for (int curNode = 0; curNode < numNodes; curNode++) {
            // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
            PastryNode node = factory.newNode();

            // construct a new MyApp
            MyApp app = new MyApp(node);

            apps.add(app);

            node.boot(bootaddress);

            // the node may require sending several messages to fully boot into the ring
            synchronized(node) {
                while(!node.isReady() && !node.joinFailed()) {
                    // delay so we don't busy-wait
                    node.wait(500);

                    // abort if can't join
                    if (node.joinFailed()) {
                        throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason());
                    }
                }
            }

            System.out.println("Finished creating new node "+node);
        }

        // wait 10 seconds
        env.getTimeSource().sleep(10000);


        MyApp foo = (MyApp)apps.get(0);
        System.out.println(((PastryNode)foo.node).getRoutingTable().printSelf());

//        System.exit(0);

        // route 10 messages
        for (int i = 0; i < 10; i++) {

            // for each app
            Iterator<MyApp> appIterator = apps.iterator();
            while(appIterator.hasNext()) {
                MyApp app = appIterator.next();

                // pick a key at random
                Id randId = nidFactory.generateNodeId();

                // send to that key
                app.routeMyMsg(randId);

                // wait a bit
                env.getTimeSource().sleep(100);
            }
        }
        // wait 1 second
        env.getTimeSource().sleep(1000);

        // for each app
        Iterator<MyApp> appIterator = apps.iterator();
        while(appIterator.hasNext()) {
            MyApp app = appIterator.next();
            PastryNode node = (PastryNode)app.getNode();

            // send directly to my leafset
            LeafSet leafSet = node.getLeafSet();

            // this is a typical loop to cover your leafset.  Note that if the leafset
            // overlaps, then duplicate nodes will be sent to twice
            for (int i=-leafSet.ccwSize(); i<=leafSet.cwSize(); i++) {
                if (i != 0) { // don't send to self
                    // select the item
                    NodeHandle nh = leafSet.get(i);

                    // send the message directly to the node
                    app.routeMyMsgDirect(nh);

                    // wait a bit
                    env.getTimeSource().sleep(100);
                }
            }
        }
    }

    /**
     * Usage:
     * java [-cp FreePastry-<version>.jar] rice.tutorial.lesson4.DistTutorial localbindport bootIP bootPort numNodes
     * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10
     */
    public static void main(String[] args) throws Exception {
        // Loads pastry settings
        Environment env = new Environment();

        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        env.getParameters().setString("nat_search_policy","never");

        try {
            // the port to use locally
            int bindport = Integer.parseInt(args[0]);

            // build the bootaddress from the command line args
            InetAddress bootaddr = InetAddress.getByName(args[1]);
            int bootport = Integer.parseInt(args[2]);
            InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);

            // the number of nodes to use
            int numNodes = Integer.parseInt(args[3]);

            // launch our node!
            DistTutorial dt = new DistTutorial(bindport, bootaddress, numNodes, env);
        } catch (Exception e) {
            // remind user how to use
            System.out.println("Usage:");
            System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.lesson4.DistTutorial localbindport bootIP bootPort numNodes");
            System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10");
            throw e;
        }
    }
}
