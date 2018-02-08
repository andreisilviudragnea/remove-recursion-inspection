# Remove Recursion Inspection

This inspection extends the functionality of the "Tail Recursion" inspection
in the "Performance" group for Java in Intellij IDEA.

This inspection detects methods containing recursive calls (not just tail recursive 
calls) and removes the recursion from the method body, while preserving the original
semantics of the code. However, the resulting code becomes rather obfuscated if the 
control flow in the recursive method is complex.

## Repository structure
- The [inspection](inspection) directory contains the source code of the inspection.
- A detailed description of the algorithm is included in [thesis/thesis.pdf](thesis/thesis.pdf).
