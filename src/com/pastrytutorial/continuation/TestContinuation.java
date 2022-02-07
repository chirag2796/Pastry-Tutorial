package com.pastrytutorial.continuation;
import rice.Continuation;
import rice.p2p.past.Past;
import rice.pastry.Id;

public class TestContinuation {
    public static void main(String[] args) {
        Past past = null; // generated elsewhere
        Id id = null; // generated elsewhere

        // create the continuation
        Continuation command = new MyContinuation();

        // make the call with the continuation
        past.lookup(id, command);
    }
}
