# WeightedMatching

## Some rules for input graphs

* No multiedges
* No loops

## How to test

* ./gradlew test -- runs unit tests
* ./gradlew run -- reads graph from stdin and prints the weight of maximum matching

## Graphs-file format
N M  
from<sub>0</sub> to<sub>0</sub> weight<sub>0</sub>  
...  
from<sub>M - 1</sub> to<sub>M - 1</sub> weight<sub>M - 1</sub>  
<EOF>  
Where N is the number of vertices, M is the number of edges. from<sub>i</sub>/to<sub>i</sub> are from 0 to N - 1.  
