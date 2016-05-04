package org.sigmah.server.dao;

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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sigmah.server.dao.util.SQLDialect;
import org.sigmah.server.dao.util.SiteOrder;
import org.sigmah.server.dao.util.SiteProjectionBinder;
import org.sigmah.server.dao.util.SiteTableColumn;
import org.sigmah.server.dao.util.SqlQueryBuilder;
import org.sigmah.server.domain.AdminEntity;
import org.sigmah.server.domain.AdminLevel;
import org.sigmah.server.domain.Bounds;
import org.sigmah.server.domain.User;
import org.sigmah.shared.dto.AdminLevelDTO;
import org.sigmah.shared.dto.IndicatorDTO;
import org.sigmah.shared.dto.referential.DimensionType;

import com.google.gwt.core.client.GWT;
import org.sigmah.shared.util.Filter;

/**
 * SQL implementation of {@link SiteTableDAO}.
 * Was made for the Google Gear version of the offline mode.
 * 
 * @author Alexander Bertram (akbertram@gmail.com)
 * @author Denis Colliot (dcolliot@ideia.fr)
 */
public class SqlSiteTableDAO implements SiteTableDAO {

	// TODO [SqlSiteTableDAO] Replace native SQL with constants (see EntityConstants).

	private final Connection connection;
	private final SQLDialect dialect;

	public SqlSiteTableDAO(Connection connection, SQLDialect dialect) {
		this.connection = connection;
		this.dialect = dialect;
	}

	/**
	 * @param <RowT>
	 *          The type of the data structure used to store the results of the query.
	 * @param filter
	 *          Filter to apply to the "base" query.
	 * @param orderings
	 *          orderings to apply to the "base" query.
	 * @param binder
	 *          Instanceof {@link SiteProjectionBinder} responsible for binding the results of the query to the
	 *          <code>RowT</code> data structure.
	 * @param retrieve
	 *          Bitmask of additional entities to flatten and retrieve: RETRIEVE_ALL, RETRIEVE_NONE, RETRIEVE_ADMIN,
	 *          RETRIEVE_INDICATORS, RETRIEVE_ATTRIBS.
	 * @param offset
	 *          For paged queries, the first row to retrieve (0-based).
	 * @param limit
	 *          For paged queries, the maximum number of rows to retrieve.
	 * @return the query results list.
	 */
	@Override
	public <RowT> List<RowT> query(User user, Filter filter, List<SiteOrder> orderings, final SiteProjectionBinder<RowT> binder, final int retrieve, int offset,
			int limit) {

		try {
			Integer databaseId = null;
			for (Integer restriction : filter.getRestrictions(DimensionType.Database)) {
				databaseId = restriction;
				break;
			}

			BaseQueryBuilder builder = new BaseQueryBuilder(databaseId).appendFieldList(SiteTableColumn.values());

			if (orderings != null) {
				builder.appendOrderings(orderings);
			}

			if (filter != null) {
				builder.filteredBy(filter);
			}

			if (offset > 0 || limit > 0) {
				builder.setLimitClause(dialect.limitClause(offset, limit));
			}

			final Map<Integer, RowT> siteMap = new HashMap<Integer, RowT>();
			final List<RowT> sites = new ArrayList<RowT>();

			ResultSet rs = builder.executeQuery(connection);
			while (rs.next()) {
				RowT site = binder.newInstance(builder.aliases(), rs);
				sites.add(site);
				if (retrieve != 0) {
					siteMap.put(rs.getInt(SiteTableColumn.id.index()), site);
				}
			}

			if (!sites.isEmpty()) {

				if ((retrieve & RETRIEVE_ADMIN) != 0) {
					joinAdminEntities(siteMap, binder);
				}
				if ((retrieve & RETRIEVE_ATTRIBS) != 0) {
					this.joinAttributeValues(siteMap, binder);
				}
				if ((retrieve & RETRIEVE_INDICATORS) != 0) {
					joinIndicatorValues(siteMap, binder);
				}
			}

			return sites;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int queryCount(User user, Filter filter) {
		try {
			BaseQueryBuilder builder = new BaseQueryBuilder(user.getId());
			builder.appendField("count(*)");

			if (filter != null) {
				builder.filteredBy(filter);
			}

			ResultSet rs = builder.executeQuery(connection);

			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int queryPageNumber(User user, Filter filter, List<SiteOrder> orderings, int pageSize, int siteId) {
		try {
			BaseQueryBuilder builder = new BaseQueryBuilder(user.getId());
			builder.appendField("site.SiteId");

			if (orderings != null) {
				builder.appendOrderings(orderings);
			}

			if (filter != null) {
				builder.filteredBy(filter);
			}

			ResultSet rs = builder.executeQuery(connection);

			int index = 0;
			while (rs.next()) {
				if (rs.getInt(1) == siteId) {
					return index / pageSize; // java integer division rounds down to zero
				}
				index++;
			}

			return -1;

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected <SiteT> void joinAdminEntities(Map<Integer, SiteT> siteMap, SiteProjectionBinder<SiteT> binder) throws SQLException {
		Map<Integer, AdminEntity> adminEntities = queryEntities(siteMap);
		linkAdminEntitiesToSites(binder, siteMap, adminEntities);
	}

	private <SiteT> Map<Integer, AdminEntity> queryEntities(Map<Integer, SiteT> siteMap) throws SQLException {
		Map<Integer, AdminEntity> map = new HashMap<Integer, AdminEntity>();

		ResultSet rs =
				SqlQueryBuilder
					.select("e.AdminEntityId, e.Name, e.AdminEntityParentId, e.AdminLevelId, e.X1, e.Y1, e.X2, e.Y2")
					.from("AdminEntity e")
					.where("e.AdminEntityId")
					.in(
						SqlQueryBuilder.select("AdminEntityId").from("LocationAdminLink").where("LocationId")
							.in(SqlQueryBuilder.select("LocationId").from("Site").where("SiteId").in(siteMap.keySet()))).executeQuery(connection);

		while (rs.next()) {
			AdminEntity entity = new AdminEntity();
			entity.setId(rs.getInt(1));
			entity.setName(rs.getString(2));

			AdminEntity parent = new AdminEntity();
			parent.setId(rs.getInt(3));
			if (!rs.wasNull()) {
				entity.setParent(parent);
			}

			AdminLevel level = new AdminLevel();
			level.setId(rs.getInt(4));
			entity.setLevel(level);

			Bounds bounds = new Bounds();
			bounds.setX1(rs.getDouble(5));
			bounds.setY1(rs.getDouble(6));
			bounds.setX2(rs.getDouble(7));
			bounds.setY2(rs.getDouble(8));
			if (!rs.wasNull()) {
				entity.setBounds(bounds);
			}

			map.put(entity.getId(), entity);
		}

		return map;
	}

	private <SiteT> void linkAdminEntitiesToSites(SiteProjectionBinder<SiteT> binder, Map<Integer, SiteT> siteMap, Map<Integer, AdminEntity> adminEntities)
			throws SQLException {
		ResultSet rs =
				SqlQueryBuilder
					.select("Site.SiteId, Link.AdminEntityId ")
					.from(
						"Site INNER JOIN Location ON (Location.LocationId = Site.LocationId) "
							+ "INNER JOIN LocationAdminLink Link ON (Link.LocationId = Location.LocationId) ").where("Site.SiteId").in(siteMap.keySet())
					.executeQuery(connection);

		while (rs.next()) {
			int siteId = rs.getInt(1);
			int entityId = rs.getInt(2);
			AdminEntity adminEntity = adminEntities.get(entityId);

			binder.setAdminEntity(siteMap.get(siteId), adminEntity);
		}
	}

	protected <SiteT> void joinIndicatorValues(Map<Integer, SiteT> siteMap, SiteProjectionBinder<SiteT> binder) throws SQLException {

		ResultSet rs =
				SqlQueryBuilder
					.select("P.SiteId, V.IndicatorId, V.Value")
					.from(
						"ReportingPeriod P "
							+ "INNER JOIN IndicatorValue V ON (P.ReportingPeriodId = V.ReportingPeriodId) "
							+ "INNER JOIN Indicator I ON (I.IndicatorId = V.IndicatorId)").where("P.SiteId").in(siteMap.keySet()).and("I.dateDeleted IS NULL")
					.executeQuery(connection);

		while (rs.next()) {
			int siteId = rs.getInt(1);
			int indicatorId = rs.getInt(2);
			double indicatorValue = rs.getDouble(3);
			if (!rs.wasNull()) {
				binder.addIndicatorValue(siteMap.get(siteId), indicatorId, 0, indicatorValue);
			}
		}
	}

	protected <SiteT> void joinAttributeValues(Map<Integer, SiteT> siteMap, SiteProjectionBinder<SiteT> transformer) throws SQLException {

		ResultSet rs =
				SqlQueryBuilder.select("V.SiteId, V.AttributeId, V.Value").from("AttributeValue V").where("V.SiteId").in(siteMap.keySet()).and("NOT V.Value is NULL")
					.executeQuery(connection);

		while (rs.next()) {
			int siteId = rs.getInt(1);
			int attributeId = rs.getInt(2);
			boolean value = rs.getBoolean(3);

			transformer.setAttributeValue(siteMap.get(siteId), attributeId, value);
		}
	}

	/**
	 * Constructs the "base query" that is common to all the queryXX methods Here, the base query is a list of records
	 * from the Site table with all the tables that are Many-to-One with the SiteTable. This includes:
	 * <ul>
	 * <li>Site</li>
	 * <li>Location</li>
	 * <li>Partner (now: OrgUnit)</li>
	 * <li>Activity → UserDatabase</li>
	 * </ul>
	 * 
	 * @return an SQLQueryBuilder instance
	 */
	private class BaseQueryBuilder extends SqlQueryBuilder {

		private String[] aliases;

		private BaseQueryBuilder(int databaseId) {
			from(
				"Site  "
					+ " LEFT JOIN Activity ON (Site.ActivityId = Activity.ActivityId) "
					+ " LEFT JOIN UserDatabase ON (Site.DatabaseId = UserDatabase.DatabaseId) "
					+ " LEFT JOIN Location ON (Site.LocationId = Location.LocationId) "
					+ " LEFT JOIN LocationType ON (Location.LocationTypeId = LocationType.LocationTypeId) "
					+ " LEFT JOIN Partner ON (Site.PartnerId = Partner.PartnerId) ").whereTrue("Site.dateDeleted IS NULL").and("Activity.dateDeleted IS NULL")
				.and("UserDatabase.dateDeleted IS NULL");

			// Permissions
			if (!GWT.isClient()) {
				whereClause.append(" AND UserDatabase.DatabaseId=?");
				parameters.add(databaseId);
			}
		}

		public BaseQueryBuilder appendFieldList(SiteTableColumn[] columns) {
			aliases = new String[columns.length];
			int aliasIndex = 0;

			for (SiteTableColumn column : columns) {
				if (fieldList.length() != 0) {
					fieldList.append(", ");
				}
				fieldList.append(column.property()).append(" AS ").append(column.alias());
				aliases[aliasIndex] = column.alias();
			}
			return this;
		}

		public BaseQueryBuilder appendOrderings(List<SiteOrder> orderings) {
			for (SiteOrder order : orderings) {
				String expr;
				if (order.getColumn().startsWith(AdminLevelDTO.PROPERTY_PREFIX)) {
					int adminLevelId = AdminLevelDTO.levelIdForPropertyName(order.getColumn());
					expr = adminOrdering(adminLevelId);

				} else if (order.getColumn().startsWith(IndicatorDTO.PROPERTY_PREFIX)) {
					int indicatorId = IndicatorDTO.indicatorIdForPropertyName(order.getColumn());
					expr = indicatorOrdering(indicatorId);

				} else {
					expr = order.getColumn();
				}
				if (orderByClause.length() != 0) {
					orderByClause.append(", ");
				}
				orderByClause.append(expr);
				if (order.isDescending()) {
					orderByClause.append(" DESC");
				}
			}
			return this;
		}

		private String indicatorOrdering(int indicatorId) {
			String alias = "Indicator" + indicatorId;
			leftJoin(
				select("P.SiteId as SiteId, V.Value Value").from("Site").leftJoin("ReportingPeriod P").on("P.SiteId = Site.SiteId").leftJoin("IndicatorValue V")
					.on("V.ReportingPeriodId = P.ReportingPeriodId").where("V.IndicatorId").equalTo(indicatorId), alias).on(alias + ".SiteId = Site.SiteId");

			return alias + ".Value";
		}

		private String adminOrdering(int adminLevelId) {
			String derivedTableAlias = "Admin" + adminLevelId;
			tableList
				.append(
					"LEFT JOIN "
						+ "(SELECT Link.LocationId, Entity.Name Name FROM LocationAdminLink Link "
						+ "LEFT JOIN AdminEntity Entity ON (Link.AdminEntityId = Entity.AdminEntityId))"
						+ " AS ").append(derivedTableAlias).append("ON (").append(derivedTableAlias).append(".LocationId = Site.LocationId) ");

			return derivedTableAlias + ".Name";
		}

		@Override
		protected void addIndicatorFilter(Filter filter, DimensionType type) {
			String alias = "IndicatorLJ";
			leftJoin(
				select("P.SiteId as SiteId").from("Site").leftJoin("ReportingPeriod P").on("P.SiteId = Site.SiteId").leftJoin("IndicatorValue V")
					.on("V.ReportingPeriodId = P.ReportingPeriodId"), alias).on(alias + ".SiteId = Site.SiteId");
		}

		public String[] aliases() {
			return aliases;
		}
	}
}
