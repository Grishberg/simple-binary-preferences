package com.github.grishberg.binarypreferences;

import java.util.concurrent.Executor;

interface ExecutorProvider {
    Executor get();
}
