package com.yumesplugins.purehelper;

/** Seam over client widget lookup so hiding logic is testable. */
interface WidgetHider
{
	void setHidden(int componentId, boolean hidden);
}
