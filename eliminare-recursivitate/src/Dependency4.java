class Dependency4 {
    int calculate(int one, int two, int three) {
        return calculate(one ^ 2,one * two, one + two + three);
    }
}