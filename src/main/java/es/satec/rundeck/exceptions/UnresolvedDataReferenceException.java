package es.satec.rundeck.exceptions;

@SuppressWarnings("serial")
/**
 * Indicates that the value of a property reference could not be resolved.
 */
public class UnresolvedDataReferenceException extends RuntimeException {
	private final String template;
	private final String referenceName;

	public UnresolvedDataReferenceException(final String template, final String referenceName) {
		super(String.format("Property %s could not be resolved in template: %s", referenceName, template));
		this.template = template;
		this.referenceName = referenceName;
	}

	public String getTemplate() {
		return template;
	}

	public String getReferenceName() {
		return referenceName;
	}
}