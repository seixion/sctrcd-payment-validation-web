package com.sctrcd.payments.validation.iban;

import java.util.ArrayList;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.runtime.StatelessKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sctrcd.drools.util.DroolsResource;
import com.sctrcd.drools.util.DroolsUtil;
import com.sctrcd.drools.util.ResourcePathType;
import com.sctrcd.drools.util.TrackingAgendaEventListener;
import com.sctrcd.drools.util.TrackingWorkingMemoryEventListener;
import com.sctrcd.payments.enums.CountryEnum;
import com.sctrcd.payments.facts.Country;
import com.sctrcd.payments.facts.IbanValidationRequest;

/**
 * 
 */
@Service("ruleBasedIbanValidator")
public class RuleBasedIbanValidator implements IbanValidator {

    private static Logger log = LoggerFactory.getLogger(RuleBasedIbanValidator.class);
    
    private KnowledgeBase kbase;
    
    public final List<Country> countries = new ArrayList<Country>();
    
    public RuleBasedIbanValidator() {
        this.kbase = DroolsUtil.createKnowledgeBase(
                new DroolsResource[]{ 
                        new DroolsResource("rules/payments/validation/IbanRules.drl", 
                                ResourcePathType.CLASSPATH, 
                                ResourceType.DRL)
                }, 
                EventProcessingOption.CLOUD);
        for (CountryEnum c : CountryEnum.values()) {
            countries.add(new Country(c.isoCode, c.name));
        }
    }
        
	@Override
	public IbanValidationResult validateIban(String iban) {
	    log.debug("Validating IBAN : " + iban + "\n");
	    
	    StatelessKnowledgeSession ksession = kbase.newStatelessKnowledgeSession();
	    
	    ksession.setGlobal("countryList", countries);
	    
	    TrackingAgendaEventListener agendaEventListener = 
	            new TrackingAgendaEventListener();
	    TrackingWorkingMemoryEventListener workingMemoryEventListener = 
	            new TrackingWorkingMemoryEventListener();
	    ksession.addEventListener(agendaEventListener);
	    ksession.addEventListener(workingMemoryEventListener);
	    
	    IbanValidationRequest request = new IbanValidationRequest(iban); 
	    
	    List<Object> facts = new ArrayList<Object>();
	    facts.add(request);
	    
		ksession.execute(facts);
		
		IbanValidationResult result = new IbanValidationResult();
		result.setValid(request.isValid());
		result.setIban(iban);
		result.addAnnotations(request.getAnnotations());
		
		ksession.removeEventListener(agendaEventListener);
		ksession.removeEventListener(workingMemoryEventListener);
		
		log.debug("Validation complete for IBAN : " + iban + "\n");
		
		return result;
	}
	
}
