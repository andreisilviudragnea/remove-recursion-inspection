class Dependency3 {
    int calculate(int one, int two, int three) {
        return <caret>calculate(three + two, two + one, one);
    }
}