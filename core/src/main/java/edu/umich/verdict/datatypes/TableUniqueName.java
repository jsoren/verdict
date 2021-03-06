package edu.umich.verdict.datatypes;

import com.google.common.base.Optional;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.util.StringManipulations;

public class TableUniqueName implements Comparable<TableUniqueName> {
	
	private final String schemaName;
	
	private final String tableName;
	
	public String getDatabaseName() {
		return getSchemaName();
	}
	
	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public TableUniqueName(String schemaName, String tableName) {
		this.schemaName = (schemaName != null)? schemaName.toLowerCase() : schemaName;
		this.tableName = (tableName != null)? tableName.toLowerCase() : tableName;
	}
	
	public static TableUniqueName uname(String schemaName, String tableName) {
		return new TableUniqueName(schemaName, tableName);
	}
	
	public static TableUniqueName uname(VerdictContext vc, String tableName) {
		Optional<String> schema = StringManipulations.schemaOfTableName(vc.getCurrentSchema(), tableName);
		String table = StringManipulations.tableNameOfTableName(tableName);
		if (schema.isPresent()) {
			return new TableUniqueName(schema.get(), table);
		} else {
			return new TableUniqueName(null, table);
		}
	}
	
	public String fullyQuantifiedName() {
		return fullyQuantifiedName(schemaName, tableName);
	}
	
	public static String fullyQuantifiedName(String schema, String table) {
		return StringManipulations.fullTableName(Optional.fromNullable(schema), table);
	}
	
	@Override
	public int hashCode() {
		if (schemaName == null) {
			return tableName.hashCode();
		} else {
			return schemaName.hashCode() + tableName.hashCode(); 
		}
	}
	
	@Override
	public boolean equals(Object another) {
		if (another instanceof TableUniqueName) {
			TableUniqueName t = (TableUniqueName) another;
			if (schemaName == null && t.getSchemaName() == null) {
				return tableName.equals(t.tableName);
			} else if (schemaName != null && t.getSchemaName() != null) {
				return schemaName.equals(t.getSchemaName()) && tableName.equals(t.getTableName());
			}
			return false;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return fullyQuantifiedName(schemaName, tableName);
	}

	public int compareTo(TableUniqueName another) {
		if (this.schemaName.compareTo(another.schemaName) == 0) {
			return this.tableName.compareTo(another.tableName);
		} else {
			return this.schemaName.compareTo(another.schemaName);
		}
	}
}