package zingg.common.client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for all Zingg Exceptions
 *
 * @author sgoyal
 *
 */

public class ZinggClientException extends Throwable {

	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(ZinggClientException.class);

	public String message;

	public ZinggClientException(String m) {
		super(m);
		this.message = m;
		LOG.error(m, this);
	}

	public ZinggClientException(String m, Throwable cause) {
		super(m, cause);
		this.message = m;
		LOG.error(m, cause);
	}

	public ZinggClientException(Throwable cause) {
		super(cause);
		this.message = cause.getMessage();
		LOG.error(cause.getMessage(), cause);
	}
}