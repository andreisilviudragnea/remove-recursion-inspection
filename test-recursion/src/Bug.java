public class Bug {
  public static void main(String[] args) {
    int i, j, k;
    for (i = 0, j = 0, k = 0; i < 3 && j < 3 && k < 3; i++, j++, k++) {
      System.out.println(i + " " + j + " " + k);
    }
  }
}
