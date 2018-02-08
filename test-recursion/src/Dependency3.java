class Dependency3 {
    int calculate(int one, int two, int three) {
        return calculate(three + two, two + one, one);
    }
}