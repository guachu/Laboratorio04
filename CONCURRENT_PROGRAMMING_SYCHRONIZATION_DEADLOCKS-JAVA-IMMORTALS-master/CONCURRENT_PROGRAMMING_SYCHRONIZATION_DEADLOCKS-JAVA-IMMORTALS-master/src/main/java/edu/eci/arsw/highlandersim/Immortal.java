package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());

    //
    private AtomicBoolean isPaused;
    private Thread originalThread;
    private final AtomicBoolean isStopped;
    
    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb, AtomicBoolean isPaused, AtomicBoolean isStopped, Thread originalThread) {
        super(name);
        this.updateCallback = ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue = defaultDamageValue;
        this.isPaused = isPaused;
        this.isStopped = isStopped;
        this.originalThread = originalThread;
    }
    
    public boolean isDead() {
        return health <= 0;
    }
    
    @Override
    public void run() {
        while (!this.isDead() && !this.isStopped.get()) {
            while (isPaused.get()) {
                ControlFrame.reportImmortalPaused();
                
                synchronized (originalThread) {
                    try {
                        originalThread.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Immortal.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                ControlFrame.reportImmortalResumed();
            }

            Immortal im = null;

            while ((im == null || im == this) && immortalsPopulation.size() > 1
                    && !isStopped.get()) { // retries until is a valid immortal (dead or alive)

                int myIndex = immortalsPopulation.indexOf(this);

                int nextFighterIndex = r.nextInt(immortalsPopulation.size());

                //avoid self-fight
                if (nextFighterIndex == myIndex) {
                    nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
                }

                im = immortalsPopulation.get(nextFighterIndex);
            }

            if (im != this && im != null) {
                this.fight(im);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        assert this.isDead() || isStopped.get();
        ImmortalC.getInstance().removeDeadImmortal(this);
    }

    public void fight(Immortal i2) {
        boolean shouldModify;
        synchronized (i2) {
            shouldModify = i2.getHealth() > 0;
            if (shouldModify) {
                i2.changeHealth(i2.getHealth() - defaultDamageValue);
            }
        }

        if (shouldModify) {
            synchronized (this) {
                this.changeHealth(this.getHealth() + defaultDamageValue);
            }

            updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
        } else {
            updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
        }
    }

    public synchronized void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }
    
    


}
