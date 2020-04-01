package ca.uhn.fhir.jpa.empi.svc;

/*-
 * #%L
 * hapi-fhir-empi-jpalink
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.jpa.api.EmpiLinkSourceEnum;
import ca.uhn.fhir.jpa.api.EmpiMatchResultEnum;
import ca.uhn.fhir.jpa.api.IEmpiLinkSvc;
import ca.uhn.fhir.jpa.api.MatchedTargetCandidate;
import ca.uhn.fhir.jpa.empi.entity.EmpiLink;
import ca.uhn.fhir.jpa.empi.util.PersonUtil;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Lazy
@Service
public class EmpiLinkSvcImpl implements IEmpiLinkSvc {

	@Autowired
	private EmpiResourceDaoSvc myEmpiResourceDaoSvc;
	@Autowired
	private EmpiLinkDaoSvc myEmpiLinkDaoSvc;
	@Autowired
	private PersonUtil myPersonUtil;
	@Autowired
	private ResourceTableHelper myResourceTableHelper;

	@Override
	@Transactional
	public void updateLink(IBaseResource thePerson, IBaseResource theResource, EmpiMatchResultEnum theMatchResult, EmpiLinkSourceEnum theLinkSource) {
		IIdType resourceId = theResource.getIdElement().toUnqualifiedVersionless();

		validateRequestIsLegal(theResource, theMatchResult, theLinkSource);

		switch (theMatchResult) {
			case MATCH:
				// FIXME EMPI use assurance 2 for possible and assurance 4 for no match
			case POSSIBLE_MATCH:
				if (!myPersonUtil.containsLinkTo(thePerson, resourceId)) {
					myPersonUtil.addLink( thePerson, resourceId);
					myEmpiResourceDaoSvc.updatePerson(thePerson);
				}
				break;
			case NO_MATCH:
				if (myPersonUtil.containsLinkTo(thePerson, resourceId)) {
					myPersonUtil.removeLink(thePerson, resourceId);
					myEmpiResourceDaoSvc.updatePerson(thePerson);
				}
		}
		createOrUpdateLinkEntity(thePerson, theResource, theMatchResult, theLinkSource);
	}

	/**
	 * Helper function which runs various business rules about what types of requests are allowed.
	 */
	private void validateRequestIsLegal(IBaseResource theResource, EmpiMatchResultEnum theMatchResult, EmpiLinkSourceEnum theLinkSource) {
		EmpiLink existingLink = getEmpiLinkForResourceTarget(theResource);
		if (existingLink != null && systemIsAttemptingToModifyManualLink(theLinkSource, existingLink.getLinkSource())) {
			throw new InternalErrorException("EMPI system is not allowed to modify links on manually created links");
		}

		if (systemIsAttemptingToAddNoMatch(theLinkSource, theMatchResult)) {
			throw new InternalErrorException("EMPI system is not allowed to automatically NO_MATCH a resource");
		}
	}

	/**
	 * Helper function which detects when the EMPI system is attempting to add a NO_MATCH link, which is not allowed.
	 */
	private boolean systemIsAttemptingToAddNoMatch(EmpiLinkSourceEnum theLinkSource, EmpiMatchResultEnum theMatchResult) {
		return EmpiLinkSourceEnum.AUTO.equals(theLinkSource) && EmpiMatchResultEnum.NO_MATCH.equals(theMatchResult);
	}

	/**
	 * Helper function to let us catch when System EMPI rules are attempting to override a manually defined link.
	 */
	private boolean systemIsAttemptingToModifyManualLink(EmpiLinkSourceEnum theIncomingSource, EmpiLinkSourceEnum theExistingSource) {
		return EmpiLinkSourceEnum.AUTO.equals(theIncomingSource) && EmpiLinkSourceEnum.MANUAL.equals(theExistingSource);
	}

	@Override
	@Transactional
	public void updateLinks(IBaseResource theIncomingResource, List<MatchedTargetCandidate> theMatchedResults, EmpiLinkSourceEnum theLinkSource) {
		//FIXME EMPI
		//Given theIncomingResource, attempt to find a person. //QUESTION GGG: How do we determine a person match?
		theMatchedResults.stream()
			.filter(mr -> mr.getMatchResult().equals(EmpiMatchResultEnum.MATCH))
			.findFirst()
			.ifPresent(mr -> {
				EmpiLink empiLink = getEmpiLinkForResourceTarget(mr.getCandidate());
				IBaseResource person = myEmpiResourceDaoSvc.readPerson(empiLink.getPerson().getIdDt());
				this.updateLink(person, theIncomingResource, mr.getMatchResult(), EmpiLinkSourceEnum.AUTO);
			});
		//If person is found, that is our person. If not, create a new person with information from theIncomingResource

		//Link theIncomingResource to the person if this is not already done
		//Given the match results, create links where appropriate from our found person to the candidate.
	}

	private EmpiLink getEmpiLinkForResourceTarget(IBaseResource theCandidate) {
		return myEmpiLinkDaoSvc.getLinkByTargetResourceId(myResourceTableHelper.getPidOrNull(theCandidate));
	}

	private void createOrUpdateLinkEntity(IBaseResource thePerson, IBaseResource theResource, EmpiMatchResultEnum theMatchResult, EmpiLinkSourceEnum theLinkSource) {
		myEmpiLinkDaoSvc.createOrUpdateLinkEntity(thePerson, theResource, theMatchResult, theLinkSource);
	}



}