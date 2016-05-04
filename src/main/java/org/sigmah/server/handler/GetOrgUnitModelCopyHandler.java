package org.sigmah.server.handler;

/*
 * #%L
 * Sigmah
 * %%
 * Copyright (C) 2010 - 2016 URD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.sigmah.server.dispatch.impl.UserDispatch.UserExecutionContext;
import org.sigmah.server.domain.OrgUnitModel;
import org.sigmah.server.domain.category.CategoryType;
import org.sigmah.server.domain.element.BudgetElement;
import org.sigmah.server.domain.element.BudgetSubField;
import org.sigmah.server.domain.element.FlexibleElement;
import org.sigmah.server.domain.element.QuestionChoiceElement;
import org.sigmah.server.domain.element.QuestionElement;
import org.sigmah.server.domain.layout.LayoutConstraint;
import org.sigmah.server.domain.layout.LayoutGroup;
import org.sigmah.server.handler.base.AbstractCommandHandler;
import org.sigmah.server.servlet.exporter.models.Realizer;
import org.sigmah.shared.command.GetOrgUnitModelCopy;
import org.sigmah.shared.dispatch.CommandException;
import org.sigmah.shared.dto.OrgUnitModelDTO;
import org.sigmah.shared.dto.referential.ProjectModelStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for {@link GetOrgUnitModelCopy} command
 * 
 * @author Maxime Lombard (mlombard@ideia.fr) (v2.0)
 */
public class GetOrgUnitModelCopyHandler extends AbstractCommandHandler<GetOrgUnitModelCopy, OrgUnitModelDTO> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(GetOrgUnitModelCopyHandler.class);

	/**
	 * The map of imported objects (original object, transformed object)
	 */
	private static final Map<Object, Object> modelesReset = new HashMap<Object, Object>();

	/**
	 * The list of imported objects which are transformed or being transformed.
	 */
	private static final Set<Object> modelesImport = new HashSet<Object>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OrgUnitModelDTO execute(final GetOrgUnitModelCopy cmd, final UserExecutionContext context) throws CommandException {

		LOG.debug("Duplicating Organizational unit model for command: {}", cmd);

		final Integer modelId = cmd.getModelId();

		final OrgUnitModel orgUnitModel = em().find(OrgUnitModel.class, modelId);

		if (orgUnitModel == null) {
			LOG.debug("Organizational unit model id#{} doesn't exist.", modelId);
			throw new CommandException("OrgUnit with id #" + modelId + " does not exist.");
		}

		LOG.debug("Found organizational unit model {}.", modelId);

		final OrgUnitModel copyOrgUnitModel = Realizer.realize(orgUnitModel);
		copyOrgUnitModel.resetImport(true);
		copyOrgUnitModel.setStatus(ProjectModelStatus.DRAFT);
		saveFlexibleElement(copyOrgUnitModel, em());
		copyOrgUnitModel.setName(cmd.getNewModelName());
		copyOrgUnitModel.setOrganization(orgUnitModel.getOrganization());
		em().persist(copyOrgUnitModel);

		return mapper().map(copyOrgUnitModel, new OrgUnitModelDTO(), cmd.getMappingMode());
	}

	/**
	 * Saves the flexible elements of the imported organizational unit model.
	 * 
	 * @param orgUnitModel
	 *          The imported organizational unit model.
	 * @param em
	 *          The entity manager.
	 */
	private static void saveFlexibleElement(OrgUnitModel orgUnitModel, EntityManager em) {
		// OrgUnitModel → Banner → Layout → Groups → Constraints
		if (orgUnitModel.getBanner() != null && orgUnitModel.getBanner().getLayout() != null) {
			List<LayoutGroup> bannerLayoutGroups = orgUnitModel.getBanner().getLayout().getGroups();
			if (bannerLayoutGroups != null) {
				for (LayoutGroup layoutGroup : bannerLayoutGroups) {
					List<LayoutConstraint> layoutConstraints = layoutGroup.getConstraints();
					if (layoutConstraints != null) {
						for (LayoutConstraint layoutConstraint : layoutConstraints) {
							if (layoutConstraint.getElement() != null) {
								if (layoutConstraint.getElement() instanceof QuestionElement) {
									List<QuestionChoiceElement> questionChoiceElements = ((QuestionElement) layoutConstraint.getElement()).getChoices();
									CategoryType type = ((QuestionElement) layoutConstraint.getElement()).getCategoryType();
									if (questionChoiceElements != null || type != null) {

										FlexibleElement parent = layoutConstraint.getElement();
										((QuestionElement) parent).setChoices(null);
										((QuestionElement) parent).setCategoryType(null);
										em.persist(parent);

										// Save QuestionChoiceElement with their QuestionElement parent(saved above)
										if (questionChoiceElements != null) {
											for (QuestionChoiceElement questionChoiceElement : questionChoiceElements) {
												if (questionChoiceElement != null) {
													questionChoiceElement.setId(null);
													questionChoiceElement.setParentQuestion((QuestionElement) parent);
													
													// BUGFIX #652: Removed the duplication of the category element.
													em.persist(questionChoiceElement);
												}
											}
											// Set saved QuestionChoiceElement to QuestionElement parent and update it
											((QuestionElement) parent).setChoices(questionChoiceElements);
										}

										// Save the Category type of QuestionElement parent(saved above)
										if (type != null) {
											// Set the saved CategoryType to QuestionElement parent and update it
											((QuestionElement) parent).setCategoryType(type);
										}
										// Update the QuestionElement parent
										em.merge(parent);
									} else {
										em.persist(layoutConstraint.getElement());
									}
								} else if (layoutConstraint.getElement() instanceof BudgetElement) {
									List<BudgetSubField> budgetSubFields = ((BudgetElement) layoutConstraint.getElement()).getBudgetSubFields();
									if (budgetSubFields != null) {
										FlexibleElement parent = layoutConstraint.getElement();
										((BudgetElement) parent).setBudgetSubFields(null);
										((BudgetElement) parent).setRatioDividend(null);
										((BudgetElement) parent).setRatioDivisor(null);

										for (BudgetSubField budgetSubField : budgetSubFields) {
											if (budgetSubField != null) {
												budgetSubField.setId(null);
												if (budgetSubField.getType() != null) {
													switch (budgetSubField.getType()) {
														case PLANNED:
															((BudgetElement) parent).setRatioDivisor(budgetSubField);
															break;
														case RECEIVED:
															break;
														case SPENT:
															((BudgetElement) parent).setRatioDividend(budgetSubField);
															break;
														default:
															break;

													}
												}
												budgetSubField.setBudgetElement((BudgetElement) parent);

												em.persist(budgetSubField);
											}
										}
										em.persist(parent);
									} else {
										em.persist(layoutConstraint.getElement());
									}

								} else {
									em.persist(layoutConstraint.getElement());
								}
							}
						}
					}
				}
			}
		}
		// OrgUnitModel → Detail → Layout → Groups → Constraints
		if (orgUnitModel.getDetails() != null && orgUnitModel.getDetails().getLayout() != null) {
			List<LayoutGroup> detailLayoutGroups = orgUnitModel.getDetails().getLayout().getGroups();
			if (detailLayoutGroups != null) {
				for (LayoutGroup layoutGroup : detailLayoutGroups) {
					List<LayoutConstraint> layoutConstraints = layoutGroup.getConstraints();
					if (layoutConstraints != null) {
						for (LayoutConstraint layoutConstraint : layoutConstraints) {
							if (layoutConstraint.getElement() != null) {
								if (layoutConstraint.getElement() instanceof QuestionElement) {
									List<QuestionChoiceElement> questionChoiceElements = ((QuestionElement) layoutConstraint.getElement()).getChoices();
									CategoryType type = ((QuestionElement) layoutConstraint.getElement()).getCategoryType();
									if (questionChoiceElements != null || type != null) {

										FlexibleElement parent = layoutConstraint.getElement();
										((QuestionElement) parent).setChoices(null);
										((QuestionElement) parent).setCategoryType(null);
										em.persist(parent);

										// Save QuestionChoiceElement with their QuestionElement parent(saved above)
										if (questionChoiceElements != null) {
											for (QuestionChoiceElement questionChoiceElement : questionChoiceElements) {
												if (questionChoiceElement != null) {
													questionChoiceElement.setId(null);
													questionChoiceElement.setParentQuestion((QuestionElement) parent);
													
													// BUGFIX #652: Removed the duplication of the category element.
													em.persist(questionChoiceElement);
												}
											}
											// Set saved QuestionChoiceElement to QuestionElement parent and update it
											((QuestionElement) parent).setChoices(questionChoiceElements);
										}

										// Save the Category type of QuestionElement parent(saved above)
										if (type != null) {
											// Set the saved CategoryType to QuestionElement parent and update it
											((QuestionElement) parent).setCategoryType(type);
										}
										// Update the QuestionElement parent
										em.merge(parent);
									} else {
										em.persist(layoutConstraint.getElement());
									}
								} else if (layoutConstraint.getElement() instanceof BudgetElement) {
									List<BudgetSubField> budgetSubFields = ((BudgetElement) layoutConstraint.getElement()).getBudgetSubFields();
									if (budgetSubFields != null) {
										FlexibleElement parent = layoutConstraint.getElement();
										((BudgetElement) parent).setBudgetSubFields(null);
										((BudgetElement) parent).setRatioDividend(null);
										((BudgetElement) parent).setRatioDivisor(null);

										for (BudgetSubField budgetSubField : budgetSubFields) {
											if (budgetSubField != null) {
												budgetSubField.setId(null);
												if (budgetSubField.getType() != null) {
													switch (budgetSubField.getType()) {
														case PLANNED:
															((BudgetElement) parent).setRatioDivisor(budgetSubField);
															break;
														case RECEIVED:
															break;
														case SPENT:
															((BudgetElement) parent).setRatioDividend(budgetSubField);
															break;
														default:
															break;

													}
												}
												budgetSubField.setBudgetElement((BudgetElement) parent);

												em.persist(budgetSubField);
											}
										}
										em.persist(parent);
									} else {
										em.persist(layoutConstraint.getElement());
									}

								} else {
									em.persist(layoutConstraint.getElement());
								}
							}
						}
					}
				}
			}
		}
	}

}
