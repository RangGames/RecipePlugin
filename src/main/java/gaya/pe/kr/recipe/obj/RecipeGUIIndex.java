
package gaya.pe.kr.recipe.obj;

public enum RecipeGUIIndex {
    BEFORE(46),
    AFTER(48),
    RECIPE_ITEM(50),
    CRAFT(51),
    RESULT(52);

    final int index;

    private RecipeGUIIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}

