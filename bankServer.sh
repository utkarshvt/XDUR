#!/bin/bash

echo "Params : $*"
#killall -9 java
java -Xms2048m -Xmx4096m -ea -Djava.util.logging.config.file=logging.properties -cp lib/kryo-3.0.1.jar:lib/minlog-1.2.jar:lib/objenesis-1.2.jar:lib/reflectasm-1.09-shaded.jar:/lib/mockito-all-1.8.5.jar:ppaxos.jar stm.benchmark.bank.BankServer $*
