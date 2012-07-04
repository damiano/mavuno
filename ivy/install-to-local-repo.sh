#!/bin/sh
CUR_DIR=`pwd`
cd `dirname $0`
mvn install:install-file -Dpackaging=jar -Dfile=local-repo/fanseparser-0.2.2.jar -DgroupId=edu.isi -DartifactId=fanseparser -Dversion=0.2.2

mvn install:install-file -Dpackaging=jar -Dfile=local-repo/stanford-corenlp-2012-01-08.jar -DgroupId=edu.stanford -DartifactId=stanford-corenlp -Dversion=2012-01-08

mvn install:install-file -Dpackaging=jar -Dfile=local-repo/stanford-corenlp-models-2011-12-27.jar -DgroupId=edu.stanford -DartifactId=stanford-corenlp-models -Dversion=2011-12-27
cd $CUR_DIR