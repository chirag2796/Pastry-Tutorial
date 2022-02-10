package com.pastrytutorial.scribe.maxinteger;

import rice.p2p.commonapi.*;
import rice.p2p.scribe.*;
import rice.pastry.commonapi.PastryIdFactory;

import java.util.concurrent.ThreadLocalRandom;

public class MyScribeClient implements ScribeClient, Application  {
    /**
     * The message sequence number.  Will be incremented after each send.
     */
    int seqNum = 0;

    /**
     * This task kicks off publishing and anycasting.
     * We hold it around in case we ever want to cancel the publishTask.
     */
    CancellableTask publishTask;

    /**
     * My handle to a scribe impl.
     */
    Scribe myScribe;

    /**
     * The only topic this appl is subscribing to.
     */
    Topic myTopic;

    /**
     * The Endpoint represents the underlieing node.  By making calls on the
     * Endpoint, it assures that the message will be delivered to a MyApp on whichever
     * node the message is intended for.
     */
    protected Endpoint endpoint;

    /**
     * Random Generated Integer
     */
    int randomInteger;

    /**
     * The constructor for this scribe client.  It will construct the ScribeApplication.
     *
     * @param node the PastryNode
     */
    public MyScribeClient(Node node) {
        // you should recognize this from lesson 3
        this.endpoint = node.buildEndpoint(this, "myinstance");

        // construct Scribe
        myScribe = new ScribeImpl(node,"myScribeInstance");

        // construct the topic
        myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), "example topic");
        System.out.println("myTopic = "+myTopic);

        this.randomInteger = ThreadLocalRandom.current().nextInt(1, 11);
        System.out.println("Random Integer Generated: " + this.randomInteger);

        // now we can receive messages
        endpoint.register();
    }

    /**
     * Subscribes to myTopic.
     */
    public void subscribe() {
        myScribe.subscribe(myTopic, this);
    }

    /**
     * Starts the publish task.
     */
    public void startPublishTask() {
        publishTask = endpoint.scheduleMessage(new PublishContent(), 5000, 5000);
    }


    /**
     * Part of the Application interface.  Will receive PublishContent every so often.
     */
    public void deliver(Id id, Message message) {
        if (message instanceof PublishContent) {
            sendMulticast();
            sendAnycast();
        }
    }

    /**
     * Sends the multicast message.
     */
    public void sendMulticast() {
        System.out.println("Node "+endpoint.getLocalNodeHandle()+" broadcasting "+seqNum);
        MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum);
        myScribe.publish(myTopic, myMessage);
        seqNum++;
    }

    /**
     * Called whenever we receive a published message.
     */
    public void deliver(Topic topic, ScribeContent content) {
        System.out.println("MyScribeClient.deliver("+topic+","+content+")");
        if (((MyScribeContent)content).from == null) {
            new Exception("Stack Trace").printStackTrace();
        }
    }

    /**
     * Sends an anycast message.
     */
    public void sendAnycast() {
        System.out.println("Node "+endpoint.getLocalNodeHandle()+" anycasting "+seqNum);
        MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum);
        myScribe.anycast(myTopic, myMessage);
        seqNum++;
    }

    /**
     * Called when we receive an anycast.  If we return
     * false, it will be delivered elsewhere.  Returning true
     * stops the message here.
     */
    public boolean anycast(Topic topic, ScribeContent content) {
        boolean returnValue = myScribe.getEnvironment().getRandomSource().nextInt(3) == 0;
        System.out.println("MyScribeClient.anycast("+topic+","+content+"):"+returnValue);
        return returnValue;
    }

    public void childAdded(Topic topic, NodeHandle child) {
//    System.out.println("MyScribeClient.childAdded("+topic+","+child+")");
    }

    public void childRemoved(Topic topic, NodeHandle child) {
//    System.out.println("MyScribeClient.childRemoved("+topic+","+child+")");
    }

    public void subscribeFailed(Topic topic) {
//    System.out.println("MyScribeClient.childFailed("+topic+")");
    }

    public boolean forward(RouteMessage message) {
        return true;
    }


    public void update(NodeHandle handle, boolean joined) {

    }

    class PublishContent implements Message {
        public int getPriority() {
            return MAX_PRIORITY;
        }
    }


    /************ Some passthrough accessors for the myScribe *************/
    public boolean isRoot() {
        return myScribe.isRoot(myTopic);
    }

    public NodeHandle getParent() {
        // NOTE: Was just added to the Scribe interface.  May need to cast myScribe to a
        // ScribeImpl if using 1.4.1_01 or older.
        return ((ScribeImpl)myScribe).getParent(myTopic);
        //return myScribe.getParent(myTopic);
    }

    public NodeHandle[] getChildren() {
        return myScribe.getChildren(myTopic);
    }
}
