/*
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.1
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

var QCD = QCD || {};
QCD.components = QCD.components || {};
QCD.components.elements = QCD.components.elements || {};

QCD.components.elements.LinkButton = function(_element, _mainController) {
	$.extend(this, new QCD.components.Component(_element, _mainController));

	var mainController = _mainController;
	
	var element = _element;
	
	var elementPath = this.elementPath;
	var elementName = this.elementName;
	
	var pageUrl;
	
	var button = $("#"+element.attr('id')+"_buttonDiv");
	var buttonLabel = $("#"+element.attr('id')+"_labelDiv");
	var buttonLink = $("#"+element.attr('id')+"_buttonLink");
	
	this.getComponentValue = function() {
		return null;
	}
	
	this.setComponentValue = function(value) {
		insertValue(value);
	}
	
	this.setComponentState = function(state) {
		insertValue(state);
	}
	
	function insertValue(value) {
		pageUrl = value;
	}
	
	this.setComponentEnabled = function(isEnabled) {
		if (isEnabled) {
			button.addClass('activeButton');
		} else {
			button.removeClass('activeButton');
		}
	}
	
	this.setComponentLoading = function(isLoadingVisible) {

	}
	
	function onButtonClick(e) {
		buttonLink.blur();
		if (button.hasClass('activeButton')) {
			mainController.goToPage(pageUrl);
		}
	}
	
	function constructor(_this) {
		var labelToTranslate = mainController.getPluginIdentifier()+"."+mainController.getViewName()+"."+elementPath.replace(/-/g,".")+".label";
		buttonLabel.html(mainController.getTranslation(labelToTranslate));
		buttonLink.click(onButtonClick);
	}
	
	constructor(this);
}