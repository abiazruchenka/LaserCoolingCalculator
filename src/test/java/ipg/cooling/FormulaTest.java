package ipg.cooling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaTest {

    @Test
    void compactTurnsInAndOutIntoUnicodeSubscripts() {
        assertEquals("Tᵢₙ", Formula.compact("T_in"));
        assertEquals("Pᵢₙ − Pₒᵤₜ", Formula.compact("P_in − P_out"));
        assertEquals("ΔT = Q / (ṁ·cₚ)", Formula.compact("ΔT = Q / (ṁ·c_p)"));
        assertEquals("cₚ", Formula.compact("c_p"));
        assertEquals("R_wall", Formula.compact("R_wall"));
    }
}
