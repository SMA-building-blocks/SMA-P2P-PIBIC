package peer_recommendation;

import jade.core.AID;

public class Pair {
    protected AID key;
    protected int value;

    Pair(AID key, int value){
        this.key = key;
        this.value = value;
    }
}
