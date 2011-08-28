/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.theme;

import java.util.List;

class ValueListMatcher extends ListMatcher {
	ValueListMatcher(List<String> list) {
		super(list);
	}

	@Override
	public boolean matches(List<Tag> tags) {
		for (int i = 0, n = tags.size(); i < n; ++i) {
			if (this.list.contains(tags.get(i).value)) {
				return true;
			}
		}
		return false;
	}
}