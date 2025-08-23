package dk.digitalidentity.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RiskDTO {
	private int rf;
	private int of;
	private int sf;
	private int ri;
	private int oi;
	private int si;
	private int rt;
	private int ot;
	private int st;
	private int sa;
}
