package domain;

/**
 * CLIPS-reported explanation for an action table row: the two antecedent facts that led to the recommendation and the
 * resulting consequent.
 */
public record Explanation(String previous1, String previous2, String consequent) {

    public static final Explanation EMPTY = new Explanation(null, null, null);

    public boolean isEmpty() {
        return (previous1 == null || previous1.isBlank())
                && (previous2 == null || previous2.isBlank())
                && (consequent == null || consequent.isBlank());
    }

    public boolean isPresent() {
        return !isEmpty();
    }

    public String toHtml() {
        return isEmpty() ? "" : String.format("<html>%s<br>%s<hr>%s</html>", previous1, previous2, consequent);
    }
}
