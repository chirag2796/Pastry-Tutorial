package com.pastrytutorial.scribe.maxinteger;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ScribeMaxInteger {
    /**
     * this will keep track of our Scribe applications
     */
    Vector<MyScribeClient> apps = new Vector<MyScribeClient>();

    /**
     * Based on the rice.tutorial.lesson4.DistTutorial
     *
     * This constructor launches numNodes PastryNodes. They will bootstrap to an
     * existing ring if one exists at the specified location, otherwise it will
     * start a new ring.
     *
     * @param bindport the local port to bind to
     * @param bootaddress the IP:port of the node to boot from
     * @param numNodes the number of nodes to create in this JVM
     * @param env the Environment
     */
    public ScribeMaxInteger(int bindport, InetSocketAddress bootaddress,
                            int numNodes, Environment env) throws Exception {

        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

        // construct the PastryNodeFactory, this is how we use rice.pastry.socket
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

        // loop to construct the nodes/apps
        for (int curNode = 0; curNode < numNodes; curNode++) {
            // construct a new node
            PastryNode node = factory.newNode();

            // construct a new scribe application
            MyScribeClient app = new MyScribeClient(node);
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

            System.out.println("Finished creating new node: " + node + "\n");
        }

        // for the first app subscribe then start the publishtask
        Iterator<MyScribeClient> i = apps.iterator();
        MyScribeClient app = (MyScribeClient) i.next();
        app.subscribe();
        app.startPublishTask();
        // for all the rest just subscribe
        while (i.hasNext()) {
            app = (MyScribeClient) i.next();
            app.subscribe();
        }

        // now, print the tree
        env.getTimeSource().sleep(5000);
        printTreeAndMaxInteger(apps);
    }

    /**
     * Note that this function only works because we have global knowledge. Doing
     * this in an actual distributed environment will take some more work.
     *
     * @param apps Vector of the applicatoins.
     */
    public static void printTreeAndMaxInteger(Vector<MyScribeClient> apps) {
        // build a hashtable of the apps, keyed by nodehandle
        Hashtable<NodeHandle, MyScribeClient> appTable = new Hashtable<NodeHandle, MyScribeClient>();
        Iterator<MyScribeClient> i = apps.iterator();
        while (i.hasNext()) {
            MyScribeClient app = (MyScribeClient) i.next();
            appTable.put(app.endpoint.getLocalNodeHandle(), app);
        }
        NodeHandle seed = ((MyScribeClient) apps.get(0)).endpoint
                .getLocalNodeHandle();

        // get the root
        NodeHandle root = getRoot(seed, appTable);

        // print the tree from the root down
        System.out.println("Tree: (each tab is one level down)");

        NodeHandle nodeWithMaxGeneratedInteger = getNodeWithMaxGeneratedInteger(root, 0, appTable, null);
        MyScribeClient appWithMaxGeneratedInteger = appTable.get(nodeWithMaxGeneratedInteger);
        System.out.println("The Node with Maximum Generated Integer is: " + nodeWithMaxGeneratedInteger.getId() + " with the generated Integer value of: " + appWithMaxGeneratedInteger.randomInteger);
    }

    /**
     * Recursively crawl up the tree to find the root.
     */
    public static NodeHandle getRoot(NodeHandle seed, Hashtable<NodeHandle, MyScribeClient> appTable) {
        MyScribeClient app = (MyScribeClient) appTable.get(seed);
        if (app.isRoot())
            return seed;
        NodeHandle nextSeed = app.getParent();
        return getRoot(nextSeed, appTable);
    }

    /**
     * Print's self, then children.
     */
    public static NodeHandle getNodeWithMaxGeneratedInteger(NodeHandle curNode, int recursionDepth, Hashtable<NodeHandle,
            MyScribeClient> appTable, NodeHandle maxIntegerNodeHandle) {
        // print self at appropriate tab level
        String s = "";
        for (int numTabs = 0; numTabs < recursionDepth; numTabs++) {
            s += "  ";
        }
        s += curNode.getId().toString();
        System.out.println(s);

        if(maxIntegerNodeHandle == null)
            maxIntegerNodeHandle = curNode;

        // recursively print all children
        MyScribeClient app = (MyScribeClient) appTable.get(curNode);
        int generatedInteger = app.randomInteger;
        if(generatedInteger > appTable.get(maxIntegerNodeHandle).randomInteger){
            maxIntegerNodeHandle = curNode;
        }

        NodeHandle[] children = app.getChildren();
        for (int curChild = 0; curChild < children.length; curChild++) {
            maxIntegerNodeHandle = getNodeWithMaxGeneratedInteger(children[curChild], recursionDepth + 1, appTable, maxIntegerNodeHandle);
        }
        return maxIntegerNodeHandle;
    }

    /**
     * Usage: java [-cp FreePastry- <version>.jar]
     * rice.tutorial.lesson6.ScribeTutorial localbindport bootIP bootPort numNodes
     * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
     */
    public static void main(String[] args) throws Exception {
        // Loads pastry configurations
        Environment env = new Environment();

        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        env.getParameters().setString("nat_search_policy","never");

        try {
            // the port to use locally
            int bindport = Integer.parseInt(args[0]);

            // build the bootaddress from the command line args
            InetAddress bootaddr = InetAddress.getByName(args[1]);
            int bootport = Integer.parseInt(args[2]);
            InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

            // the port to use locally
            int numNodes = Integer.parseInt(args[3]);

            // launch our node!
            ScribeMaxInteger dt = new ScribeMaxInteger(bindport, bootaddress, numNodes,  env);
            System.exit(0);
        } catch (Exception e) {
            // remind user how to use
            System.out.println("Usage:");
            System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.scribe.ScribeTutorial localbindport bootIP bootPort numNodes");
            System.out.println("example java rice.tutorial.scribe.ScribeTutorial 9001 pokey.cs.almamater.edu 9001 10");
            throw e;
        }
    }
}
