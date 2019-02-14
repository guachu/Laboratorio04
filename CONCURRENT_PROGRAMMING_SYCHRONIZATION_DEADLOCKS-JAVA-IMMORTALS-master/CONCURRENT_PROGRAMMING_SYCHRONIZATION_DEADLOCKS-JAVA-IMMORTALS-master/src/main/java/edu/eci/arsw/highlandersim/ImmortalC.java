/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aleja
 */
public class ImmortalC extends Thread{
    public static final int CYCLE_SLEEP_TIME = 100;

    private static ImmortalC instance;

    private final AtomicBoolean isStopped;
    
    private final List<Immortal> immortalList;
    private final Queue<Immortal> readyToRemove;

    private ImmortalC(List<Immortal> list, AtomicBoolean isStopped) {
        this.immortalList = list;
        readyToRemove = new ConcurrentLinkedQueue<>();
        this.isStopped = isStopped;
    }

    public static ImmortalC getInstance() {
        assert instance != null;
        return instance;
    }

    public static ImmortalC getInstance(List<Immortal> list, AtomicBoolean isStopped) {
        if (instance == null) {
            instance = new ImmortalC(list, isStopped);
        }
        return instance;
    }

    public void removeDeadImmortal(Immortal im) {
        if (im.isDead()) {
            assert immortalList.contains(im);
            readyToRemove.offer(im);
        }
    }

    @Override
    public void run() {
        while (immortalList.size() > 1 && ! isStopped.get()) {
            if (!readyToRemove.isEmpty()) {
                synchronized (immortalList) {
                    while (!readyToRemove.isEmpty()) {
                        immortalList.remove(readyToRemove.poll());
                    }
                }
            }

            try {
                Thread.sleep(CYCLE_SLEEP_TIME);
            } catch (InterruptedException ex) {
                Logger.getLogger(ImmortalC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
