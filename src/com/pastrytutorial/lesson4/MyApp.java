package com.pastrytutorial.lesson4;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;

public class MyApp implements Application {
    protected Endpoint endpoint;

    /**
     * The node we were constructed on.
     */
    protected Node node;

    public MyApp(Node node) {
        // We are only going to use one instance of this application on each PastryNode
        this.endpoint = node.buildEndpoint(this, "myinstance");

        this.node = node;

        // now we can receive messages
        this.endpoint.register();
    }

    /**
     * Getter for the node.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Called to route a message to the id
     */
    public void routeMyMsg(Id id) {
        System.out.println(this+" sending to "+id);
        Message msg = new MyMsg(endpoint.getId(), id);
        endpoint.route(id, msg, null);
    }

    /**
     * Called to directly send a message to the nh
     */
    public void routeMyMsgDirect(NodeHandle nh) {
        System.out.println(this+" sending direct to "+nh);
        Message msg = new MyMsg(endpoint.getId(), nh.getId());
        endpoint.route(null, msg, nh);
    }

    /**
     * Called when we receive a message.
     */
    public void deliver(Id id, Message message) {
        System.out.println(this+" received "+message);
    }

    /**
     * Called when you hear about a new neighbor.
     * Don't worry about this method for now.
     */
    public void update(NodeHandle handle, boolean joined) {
    }

    /**
     * Called a message travels along your path.
     * Don't worry about this method for now.
     */
    public boolean forward(RouteMessage message) {
        return true;
    }

    public String toString() {
        return "MyApp "+endpoint.getId();
    }
}
