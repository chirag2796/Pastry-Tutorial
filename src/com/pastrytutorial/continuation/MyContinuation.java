package com.pastrytutorial.continuation;
import rice.Continuation;
import rice.p2p.past.PastContent;

public class MyContinuation  implements Continuation {
    /**
     * Called when the result arrives.
     */
    public void receiveResult(Object result) {
        PastContent pc = (PastContent)result;
        System.out.println("Received a "+pc);
    }

    /**
     * Called if there is an error.
     */
    public void receiveException(Exception result) {
        System.out.println("There was an error: "+result);
    }
}
