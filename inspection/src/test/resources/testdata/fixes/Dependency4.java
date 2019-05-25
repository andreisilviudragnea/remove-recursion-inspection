class Dependency4 {
    int calculate(int one, int two, int three) {
        return <caret>calculate(one ^ 2,one * two, one + two + three);
    }
}
