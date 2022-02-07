package com.pastrytutorial.continuation;

import rice.Continuation;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.pastry.Id;

public class TestContinuationAnon {
    public static void main(String[] args) {
        Past past = null; // generated elsewhere
        Id id = null; // generated elsewhere

        // same code as TestContinuation and MyContinuation combined
        past.lookup(id, new Continuation() {
            // will be called if success in the lookup
            public void receiveResult(Object result) {
                PastContent pc = (PastContent)result;
                System.out.println("Received a "+pc);
            }

            // will be called if failure in the lookup
            public void receiveException(Exception result) {
                System.out.println("There was an error: "+result);
            }
        });
    }
}
