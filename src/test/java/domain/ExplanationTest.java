package domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationTest {

    @Test
    void emptyConstant_IsEmpty() {
        assertTrue(Explanation.EMPTY.isEmpty());
        assertFalse(Explanation.EMPTY.isPresent());
        assertThat(Explanation.EMPTY.toHtml(), is(""));
    }

    @Test
    void allNullFields_IsEmpty() {
        Explanation explanation = new Explanation(null, null, null);

        assertTrue(explanation.isEmpty());
        assertFalse(explanation.isPresent());
    }

    @Test
    void allBlankFields_IsEmpty() {
        Explanation explanation = new Explanation("   ", "\t", "  ");

        assertTrue(explanation.isEmpty());
        assertFalse(explanation.isPresent());
        assertThat(explanation.toHtml(), is(""));
    }

    @Test
    void onlyPrevious1NonEmpty_IsPresent() {
        assertTrue(new Explanation("antecedent", null, null).isPresent());
        assertTrue(new Explanation("antecedent", "  ", null).isPresent());
        assertFalse(new Explanation("antecedent", null, null).isEmpty());
    }

    @Test
    void onlyPrevious2NonEmpty_IsPresent() {
        assertTrue(new Explanation(null, "antecedent-2", null).isPresent());
        assertTrue(new Explanation("  ", "antecedent-2", "   ").isPresent());
    }

    @Test
    void onlyConsequentNonEmpty_IsPresent() {
        assertTrue(new Explanation(null, null, "consequent").isPresent());
        assertTrue(new Explanation(" ", " ", "consequent").isPresent());
    }

    @Test
    void toHtml_FormatsNonEmptyExplanation() {
        Explanation explanation = new Explanation("prev1", "prev2", "result");

        assertThat(explanation.toHtml(), is("<html>prev1<br>prev2<hr>result</html>"));
    }
}
