package com.pastrytutorial.lesson5.timer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandleSet;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class DistTutorial {
    public DistTutorial(int bindport, InetSocketAddress bootaddress, Environment env) throws Exception {

        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

        // construct the PastryNodeFactory, this is how we use rice.pastry.socket
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

        // This will return null if we there is no node at that location
        NodeHandle bootHandle = ((SocketPastryNodeFactory)factory).getNodeHandle(bootaddress);

        // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
        PastryNode node = factory.newNode();

        // construct a new MyApp
        MyApp app = new MyApp(node);

        // boot the node
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

        // wait 15 seconds
        env.getTimeSource().sleep(15000);

        // cancel the task
        app.cancelTask();
    }

    /**
     * Usage:
     * java [-cp FreePastry-<version>.jar] rice.tutorial.lesson5.DistTutorial localbindport bootIP bootPort
     * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
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

            // launch our node!
            DistTutorial dt = new DistTutorial(bindport, bootaddress, env);
        } catch (Exception e) {
            // remind user how to use
            System.out.println("Usage:");
            System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.timer.DistTutorial localbindport bootIP bootPort");
            System.out.println("example java rice.tutorial.timer.DistTutorial 9001 pokey.cs.almamater.edu 9001");
            throw e;
        }
    }
}
