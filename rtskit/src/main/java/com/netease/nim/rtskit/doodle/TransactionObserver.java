package com.netease.nim.rtskit.doodle;

import java.util.List;

/**
 * Created on 2015/6/24.
 */
public interface TransactionObserver {
    void onTransaction(List<Transaction> transactions);
}
