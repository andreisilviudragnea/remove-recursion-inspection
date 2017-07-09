package ro.pub.cs.diploma;

class Variable {
  private final String name;
  private final String type;

  Variable(String name, String type) {
    this.name = name;
    this.type = type;
  }

  String getName() {
    return name;
  }

  String getType() {
    return type;
  }
}
