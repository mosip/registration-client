package io.mosip.registration.metrics;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.id.GlobalParamId;
import io.mosip.registration.repositories.GlobalParamRepository;
import org.springframework.context.ApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TusURLGlobalParamStore implements io.tus.java.client.TusURLStore {

    private static final Logger LOGGER = AppConfig.getLogger(TusURLGlobalParamStore.class);

    private ApplicationContext applicationContext;
    private GlobalParamRepository globalParamRepository;

    public TusURLGlobalParamStore(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    public void set(String s, URL url) {
        GlobalParamId globalParamId = new GlobalParamId();
        globalParamId.setCode(s);
        globalParamId.setLangCode("eng");
        GlobalParam globalParam = getGlobalParam(globalParamId);
        if(globalParam != null) {
            globalParam.setVal(url.toString());
            globalParam.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now()));
            getGlobalParamRepository().update(globalParam);
        }
        else {
            globalParam = new GlobalParam();
            globalParam.setGlobalParamId(globalParamId);
            globalParam.setName(s);
            globalParam.setVal(url.toString());
            globalParam.setTyp("INTERNAL");
            globalParam.setCrBy("SYSTEM");
            globalParam.setCrDtime(Timestamp.valueOf(LocalDateTime.now()));
            getGlobalParamRepository().save(globalParam);
        }
    }

    @Override
    public URL get(String s) {
        GlobalParamId globalParamId = new GlobalParamId();
        globalParamId.setCode(s);
        globalParamId.setLangCode("eng");
        GlobalParam globalParam = getGlobalParam(globalParamId);
        try {
            return (globalParam != null) ? new URL(globalParam.getVal()) : null;
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null;
    }

    private GlobalParam getGlobalParam(GlobalParamId globalParamId) {
		try {
			return getGlobalParamRepository().getOne(globalParamId);
		} catch (RuntimeException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return null;
	}

	@Override
    public void remove(String s) {
        GlobalParamId globalParamId = new GlobalParamId();
        globalParamId.setCode(s);
        globalParamId.setLangCode("eng");
        getGlobalParamRepository().deleteById(globalParamId);
    }

    public GlobalParamRepository getGlobalParamRepository() {
        if(globalParamRepository == null)
            globalParamRepository = applicationContext.getBean(GlobalParamRepository.class);
        return globalParamRepository;
    }
}
