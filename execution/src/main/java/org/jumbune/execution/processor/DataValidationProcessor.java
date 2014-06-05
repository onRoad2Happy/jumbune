package org.jumbune.execution.processor;

import static org.jumbune.execution.utils.ExecutionConstants.ERRORANDEXCEPTION;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jumbune.common.beans.DataValidationBean;
import org.jumbune.common.beans.Module;
import org.jumbune.common.beans.ServiceInfo;
import org.jumbune.common.beans.ReportsBean.ReportName;
import org.jumbune.common.utils.Constants;
import org.jumbune.common.utils.RemoteFileUtil;
import org.jumbune.datavalidation.report.DVReportGenerator;
import org.jumbune.execution.beans.Parameters;
import org.jumbune.utils.exception.JumbuneException;

import com.google.gson.Gson;

/**
 * 
 * This processor can be used for performing data validation
 * 
 * 
 */
public class DataValidationProcessor extends BaseProcessor {

	private static final Logger LOGGER = LogManager.getLogger(DataValidationProcessor.class);

	/**
	 * constructor for DataValidationProcessor
	 * @param isCommandBased
	 */
	public DataValidationProcessor(boolean isCommandBased) {
		super(isCommandBased);
	}
	

	@Override
	protected boolean execute(Map<Parameters, String> params) throws JumbuneException {
		LOGGER.info("Executing [Data Validation] Processor...");
		String dvReport = null;

		// populating data validation report
		Map<ReportName, String> report = super.getReports().getReport(Module.DATA_VALIDATION);
		try {
			final Gson gsonDV = new Gson();
			// getting params path
			String inputPath = super.getLoader().getHdfsInputPath();
			// get datavalidation bean
			DataValidationBean dataValidationBean = super.getLoader().getDataValidation();
			String fieldSeparator = dataValidationBean.getFieldSeparator();
			fieldSeparator = fieldSeparator.replaceAll(Constants.SPACE, Constants.SPACE_SEPARATOR);
			dataValidationBean.setFieldSeparator(fieldSeparator);
			final String dvBeanString = gsonDV.toJson(dataValidationBean);
			String dvFileDir = super.getLoader().getSlaveDVLocationWithPlaceHolder().substring(0,
					super.getLoader().getSlaveDVLocationWithPlaceHolder().lastIndexOf('/') + 1);
			dvReport = HELPER.remoteValidateData(super.getLoader(), inputPath, dvFileDir, dvBeanString);
			dvReport = new DVReportGenerator().generateDataValidationReport(dvReport);

			// Copy logs from slaves only when there are slaves if there are no
			// slaves then don't go for copying
			if (super.getLoader().getDVDefinition().getSlaves() != null && super.getLoader().getDVDefinition().getSlaves().size() > 0) {
				LOGGER.debug("Copying files from all nodes!!!");
				RemoteFileUtil remoteFileUtil = new RemoteFileUtil();
				remoteFileUtil.copyLogFilesToMaster(super.getLoader().getDVDefinition());
			}
			LOGGER.info("Successfully Exiting [Data Validation] Processor...");
			return true;

		} catch (Exception e) {
			Map<String, Map<String, String>> errorMap = new HashMap<String, Map<String, String>>(1);
			Map<String, String> errorMessageMap = new HashMap<String, String>(1);
			errorMessageMap.put("Could not validate data", e.getMessage());
			errorMap.put(ERRORANDEXCEPTION, errorMessageMap);

			Gson gson = new Gson();

			dvReport = gson.toJson(errorMap);
			log(params, "Exception occured during Data Validation", e);
			throw new JumbuneException("HTFException: "+e);
		} finally {
			report.put(ReportName.DATA_VALIDATION, dvReport);
		
		
			super.getReports().setCompleted(Module.DATA_VALIDATION);
		}

	}

	@Override
	protected void updateServiceInfo(ServiceInfo serviceInfo) throws JumbuneException {
		if (serviceInfo != null){
			serviceInfo.setDataValidationResultLocation(super.getLoader().getDataValidationResultLocation());
		}
	}

	@Override
	protected Module getModuleName() {
		return Module.DATA_VALIDATION;
	}

}
