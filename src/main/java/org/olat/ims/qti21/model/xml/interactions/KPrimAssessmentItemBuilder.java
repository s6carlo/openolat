/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.model.xml.interactions;


import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.appendAssociationKPrimResponseDeclaration;
import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.appendDefaultItemBody;
import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.appendDefaultOutcomeDeclarations;
import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.appendMatchInteractionForKPrim;
import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.createKPrimResponseDeclaration;
import static org.olat.ims.qti21.model.xml.AssessmentItemFactory.createResponseProcessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import org.olat.core.gui.render.StringOutput;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.AssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.AssessmentItemFactory;

import uk.ac.ed.ph.jqtiplus.group.NodeGroupList;
import uk.ac.ed.ph.jqtiplus.node.content.ItemBody;
import uk.ac.ed.ph.jqtiplus.node.content.basic.Block;
import uk.ac.ed.ph.jqtiplus.node.expression.general.BaseValue;
import uk.ac.ed.ph.jqtiplus.node.expression.general.Correct;
import uk.ac.ed.ph.jqtiplus.node.expression.general.MapResponse;
import uk.ac.ed.ph.jqtiplus.node.expression.general.Variable;
import uk.ac.ed.ph.jqtiplus.node.expression.operator.IsNull;
import uk.ac.ed.ph.jqtiplus.node.expression.operator.Match;
import uk.ac.ed.ph.jqtiplus.node.expression.operator.Sum;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.CorrectResponse;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.MatchInteraction;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.SimpleAssociableChoice;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.SimpleMatchSet;
import uk.ac.ed.ph.jqtiplus.node.item.response.declaration.ResponseDeclaration;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseCondition;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseElse;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseElseIf;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseIf;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseProcessing;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseRule;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.SetOutcomeValue;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.node.shared.FieldValue;
import uk.ac.ed.ph.jqtiplus.serialization.QtiSerializer;
import uk.ac.ed.ph.jqtiplus.types.ComplexReferenceIdentifier;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.value.BaseType;
import uk.ac.ed.ph.jqtiplus.value.DirectedPairValue;
import uk.ac.ed.ph.jqtiplus.value.SingleValue;

/**
 * 
 * Initial date: 06.01.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class KPrimAssessmentItemBuilder extends AssessmentItemBuilder {

	private boolean shuffle;
	private String question;
	protected List<String> cssClass;
	private Identifier responseIdentifier;
	private MatchInteraction matchInteraction;
	private Map<Identifier,Identifier> associations;
	
	public KPrimAssessmentItemBuilder(QtiSerializer qtiSerializer) {
		super(createAssessmentItem(), qtiSerializer);
	}
	
	public KPrimAssessmentItemBuilder(AssessmentItem assessmentItem, QtiSerializer qtiSerializer) {
		super(assessmentItem, qtiSerializer);
	}
	
	private static AssessmentItem createAssessmentItem() {
		AssessmentItem assessmentItem = AssessmentItemFactory.createAssessmentItem(QTI21QuestionType.kprim, "KPrim");
		
		NodeGroupList nodeGroups = assessmentItem.getNodeGroups();

		double maxScore = 1.0d;
		Identifier responseDeclarationId = Identifier.assumedLegal("KPRIM_RESPONSE_1");
		//define correct answer
		ResponseDeclaration responseDeclaration = createKPrimResponseDeclaration(assessmentItem, responseDeclarationId, new HashMap<>(), maxScore);
		nodeGroups.getResponseDeclarationGroup().getResponseDeclarations().add(responseDeclaration);
		
		appendDefaultOutcomeDeclarations(assessmentItem, maxScore);

		//the single choice interaction
		ItemBody itemBody = appendDefaultItemBody(assessmentItem);
		MatchInteraction matchInteraction = appendMatchInteractionForKPrim(itemBody, responseDeclarationId);
		SimpleMatchSet matchSet = matchInteraction.getSimpleMatchSets().get(0);
		Map<Identifier,Identifier> associations = new HashMap<>();
		for(SimpleAssociableChoice choice:matchSet.getSimpleAssociableChoices()) {
			associations.put(choice.getIdentifier(), QTI21Constants.WRONG_IDENTIFIER);
		}
		appendAssociationKPrimResponseDeclaration(responseDeclaration, associations, 1.0);
		
		//response processing
		ResponseProcessing responseProcessing = createResponseProcessing(assessmentItem, responseDeclarationId);
		assessmentItem.getNodeGroups().getResponseProcessingGroup().setResponseProcessing(responseProcessing);
		
		return assessmentItem;
	}
	
	@Override
	public void extract() {
		super.extract();
		extractCorrectResponse();
		extractMatchInteraction();
		
		if(getMinScoreBuilder() == null) {
			setMinScore(0.0d);
		}
		if(getMaxScoreBuilder() == null) {
			setMaxScore(1.0d);
		}
	}
	
	private void extractCorrectResponse() {
		associations = new HashMap<>();
		
		List<ResponseDeclaration> responseDeclarations = assessmentItem.getResponseDeclarations();
		if(responseDeclarations.size() == 1) {
			CorrectResponse correctResponse = responseDeclarations.get(0).getCorrectResponse();
			if(correctResponse != null) {
				List<FieldValue> values = correctResponse.getFieldValues();
				for(FieldValue value:values) {
					SingleValue sValue = value.getSingleValue();
					if(sValue instanceof DirectedPairValue) {
						DirectedPairValue dpValue = (DirectedPairValue)sValue;
						Identifier sourceId = dpValue.sourceValue();
						Identifier destinationId = dpValue.destValue();
						associations.put(sourceId, destinationId);
					}
				}
			}
		}
	}
	
	private void extractMatchInteraction() {
		StringOutput sb = new StringOutput();
		List<Block> blocks = assessmentItem.getItemBody().getBlocks();
		for(Block block:blocks) {
			if(block instanceof MatchInteraction) {
				matchInteraction = (MatchInteraction)block;
				responseIdentifier = matchInteraction.getResponseIdentifier();
				shuffle = matchInteraction.getShuffle();
				cssClass = matchInteraction.getClassAttr();
				break;
			} else {
				qtiSerializer.serializeJqtiObject(block, new StreamResult(sb));
			}
		}
		question = sb.toString();
	}
	
	@Override
	public QTI21QuestionType getQuestionType() {
		return QTI21QuestionType.kprim;
	}
	
	public boolean isShuffle() {
		return shuffle;
	}
	
	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}
	
	public boolean hasClassAttr(String classAttr) {
		return cssClass != null && cssClass.contains(classAttr);
	}
	
	public void addClass(String classAttr) {
		if(cssClass == null) {
			cssClass = new ArrayList<>();
		} 
		if(!cssClass.contains(classAttr)) {
			cssClass.add(classAttr);
		}
	}
	
	public void removeClass(String classAttr) {
		if(cssClass != null) {
			cssClass.remove(classAttr);
		}
	}

	public boolean isCorrect(Identifier choiceId) {
		Identifier mappedId = associations.get(choiceId);
		return mappedId != null && mappedId.equals(QTI21Constants.CORRECT_IDENTIFIER);
	}
	
	public boolean isWrong(Identifier choiceId) {
		Identifier mappedId = associations.get(choiceId);
		return mappedId != null && mappedId.equals(QTI21Constants.WRONG_IDENTIFIER);
	}
	
	public void setAssociation(Identifier choiceId, Identifier correctOrWrongId) {
		associations.put(choiceId, correctOrWrongId);
	}
	
	/**
	 * Return the HTML block before the choice interaction as a string.
	 * 
	 * @return
	 */
	@Override
	public String getQuestion() {
		return question;
	}

	@Override
	public void setQuestion(String html) {
		this.question = html;
	}
	
	public MatchInteraction getMatchInteraction() {
		return matchInteraction;
	}
	
	public List<SimpleAssociableChoice> getKprimChoices() {
		return matchInteraction.getSimpleMatchSets().get(0).getSimpleAssociableChoices();
	}

	@Override
	protected void buildResponseAndOutcomeDeclarations() {
		//need min. and max. score
		double maxScore = getMaxScoreBuilder().getScore();
		
		//refresh correct response
		if(assessmentItem.getResponseDeclaration(responseIdentifier) != null) {
			ResponseDeclaration responseDeclaration = assessmentItem.getResponseDeclaration(responseIdentifier);
			appendAssociationKPrimResponseDeclaration(responseDeclaration, associations, maxScore);
		} else {
			ResponseDeclaration responseDeclaration =
					createKPrimResponseDeclaration(assessmentItem, responseIdentifier, associations, maxScore);
			assessmentItem.getResponseDeclarations().add(responseDeclaration);
		}
	}
	
	@Override
	protected void buildItemBody() {
		//remove current blocks
		List<Block> blocks = assessmentItem.getItemBody().getBlocks();
		blocks.clear();

		//add question
		getHtmlHelper().appendHtml(assessmentItem.getItemBody(), question);

		matchInteraction.setShuffle(isShuffle());
		if(cssClass == null || cssClass.isEmpty()) {
			matchInteraction.setClassAttr(null);
		} else {
			matchInteraction.setClassAttr(cssClass);
		}
		
		blocks.add(matchInteraction);
	}

	@Override
	protected void buildModalFeedbacksAndHints(List<OutcomeDeclaration> outcomeDeclarations, List<ResponseRule> responseRules) {
		super.buildModalFeedbacksAndHints(outcomeDeclarations, responseRules);
		ensureFeedbackBasicOutcomeDeclaration();
	}

	@Override
	protected void buildMainScoreRule(List<OutcomeDeclaration> outcomeDeclarations, List<ResponseRule> responseRules) {
		ResponseCondition rule = new ResponseCondition(assessmentItem.getResponseProcessing());
		responseRules.add(0, rule);
		buildMainKPrimScoreRule(rule);
	}
	
	/**
	 * The score is 100% if 4 correct responses, 50% if 3 correct
	 * responses and 0 otherwise.
	 * 
	 * @param rule
	 */
	private void buildMainKPrimScoreRule(ResponseCondition rule) {
		/*
	<responseCondition>
		<responseIf>
			<isNull>
				<variable identifier="RESPONSE"/>
			</isNull>
			<setOutcomeValue identifier="SCORE">
				<baseValue baseType="float">0.0</baseValue>
			</setOutcomeValue>
		</responseIf>
		<responseElse>
			<setOutcomeValue identifier="SCORE">
				<mapResponse identifier="RESPONSE"/>
			</setOutcomeValue>
		</responseElse>
	</responseCondition>
		 */
		
		ResponseIf responseIf = new ResponseIf(rule);
		rule.setResponseIf(responseIf);
		
		{//if no response
			IsNull isNull = new IsNull(responseIf);
			responseIf.getExpressions().add(isNull);
			
			Variable variable = new Variable(isNull);
			variable.setIdentifier(ComplexReferenceIdentifier.parseString(responseIdentifier.toString()));
			isNull.getExpressions().add(variable);
			
			SetOutcomeValue incorrectOutcomeValue = new SetOutcomeValue(responseIf);
			incorrectOutcomeValue.setIdentifier(QTI21Constants.FEEDBACKBASIC_IDENTIFIER);
			responseIf.getResponseRules().add(incorrectOutcomeValue);
			
			BaseValue incorrectValue = new BaseValue(incorrectOutcomeValue);
			incorrectValue.setBaseTypeAttrValue(BaseType.IDENTIFIER);
			incorrectValue.setSingleValue(QTI21Constants.EMPTY_IDENTIFIER_VALUE);
			incorrectOutcomeValue.setExpression(incorrectValue);
		}
		
		ResponseElseIf responseElseIf = new ResponseElseIf(rule);
		rule.getResponseElseIfs().add(responseElseIf);
		
		{// match the correct answers -> 100%
			Match match = new Match(responseElseIf);
			responseElseIf.getExpressions().add(match);
			
			Variable scoreVar = new Variable(match);
			ComplexReferenceIdentifier choiceResponseIdentifier
				= ComplexReferenceIdentifier.parseString(matchInteraction.getResponseIdentifier().toString());
			scoreVar.setIdentifier(choiceResponseIdentifier);
			match.getExpressions().add(scoreVar);
			
			Correct correct = new Correct(match);
			correct.setIdentifier(choiceResponseIdentifier);
			match.getExpressions().add(correct);
		}
	
		{//outcome score
			SetOutcomeValue scoreOutcomeValue = new SetOutcomeValue(responseIf);
			scoreOutcomeValue.setIdentifier(QTI21Constants.SCORE_IDENTIFIER);
			responseElseIf.getResponseRules().add(scoreOutcomeValue);
			
			Sum sum = new Sum(scoreOutcomeValue);
			scoreOutcomeValue.getExpressions().add(sum);
			
			Variable scoreVar = new Variable(sum);
			scoreVar.setIdentifier(QTI21Constants.SCORE_CLX_IDENTIFIER);
			sum.getExpressions().add(scoreVar);
			
			Variable maxScoreVar = new Variable(sum);
			maxScoreVar.setIdentifier(QTI21Constants.MAXSCORE_CLX_IDENTIFIER);
			sum.getExpressions().add(maxScoreVar);
		}
		
		{//set outcome
			SetOutcomeValue correctOutcomeValue = new SetOutcomeValue(responseIf);
			correctOutcomeValue.setIdentifier(QTI21Constants.FEEDBACKBASIC_IDENTIFIER);
			responseElseIf.getResponseRules().add(correctOutcomeValue);
			
			BaseValue correctValue = new BaseValue(correctOutcomeValue);
			correctValue.setBaseTypeAttrValue(BaseType.IDENTIFIER);
			correctValue.setSingleValue(QTI21Constants.CORRECT_IDENTIFIER_VALUE);
			correctOutcomeValue.setExpression(correctValue);
		}

		ResponseElse responseElse = new ResponseElse(rule);
		rule.setResponseElse(responseElse);
		
		/* Not perfect ->
		 	<setOutcomeValue identifier="SCORE">
          		<sum>
            		<mapResponse identifier="KPRIM_RESPONSE_1"/>
          		</sum>
        	</setOutcomeValue>
		*/
		{// outcome feedback
			
			SetOutcomeValue halfScoreOutcomeValue = new SetOutcomeValue(responseIf);
			halfScoreOutcomeValue.setIdentifier(QTI21Constants.SCORE_IDENTIFIER);
			responseElse.getResponseRules().add(halfScoreOutcomeValue);
			
			Sum sum = new Sum(halfScoreOutcomeValue);
			halfScoreOutcomeValue.setExpression(sum);
			
			MapResponse mapResponse = new MapResponse(sum);
			mapResponse.setIdentifier(responseIdentifier);
			sum.getExpressions().add(mapResponse);
			
			SetOutcomeValue incorrectOutcomeValue = new SetOutcomeValue(responseIf);
			incorrectOutcomeValue.setIdentifier(QTI21Constants.FEEDBACKBASIC_IDENTIFIER);
			responseElse.getResponseRules().add(incorrectOutcomeValue);
			
			BaseValue incorrectValue = new BaseValue(incorrectOutcomeValue);
			incorrectValue.setBaseTypeAttrValue(BaseType.IDENTIFIER);
			incorrectValue.setSingleValue(QTI21Constants.INCORRECT_IDENTIFIER_VALUE);
			incorrectOutcomeValue.setExpression(incorrectValue);
		}
	}
}