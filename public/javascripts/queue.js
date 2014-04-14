/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/

function WorkerManager() {
	var workerlist = [];
	var processingtasks = [];

	this.prepare = function() {
		getAll();
		$("#btnrefresh").click(refresh);
		refresh_handle = setInterval(refresh, 10000);
	};

	ajaxUpdateLists = function(url, success) {
		$.ajax({
			url: url,
			type: "POST",
			async: false,
			beforeSend: function() {
				$('#imgajaxloader').css("display", "inline");
				$("#btnrefresh").css("display", "none");
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#queueplaceholder').after('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n('queue.fetcherror') + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
				clearInterval(refresh_handle);
				$('#imgajaxloader').css("display", "none");
			},
			success: function(data) {
				$('#imgajaxloader').css("display", "none");
				$("#btnrefresh").css("display", "inline");
				if (data.result) {
					success(data);
				} else {
					console.log(data);
				}
			}
		});
	};
	
	updateExcuseTextIfEmpty = function() {
		if(jQuery.isEmptyObject(workerlist)) {
			$('#wrk-list').html('<div class="alert alert-info" style="margin-bottom: 0px;">' + i18n('queue.workers.noactivity') + '</div>');
		} else {
			$('#wrk-list .alert').remove();
		}
	};

	getAll = function() {
		ajaxUpdateLists(url_getworkers, function(rawdata) {
			workerlist = rawdata.workers;
			processingtasks = rawdata.processingtasks;
			createfullworkertable();
			updateExcuseTextIfEmpty();
		});
	};
	
	createfullworkertable = function() {
		var table = [];
		
		for (var key in workerlist) {
			workerlist[key].key = key;
			table.push(workerlist[key]);
		}

		var table = table.sort(function(a, b) {
			if (a.hostname == b.hostname) {
				if ((a.cyclic & b.cyclic) | (!a.cyclic & !b.cyclic)) {
					return a.long_worker_name < b.long_worker_name ? -1 : 1;
				} else {
					return a.cyclic > b.cyclic ? -1 : 1;
				}
			} else {
				return a.hostname < b.hostname ? -1 : 1;
			}
		});
		
		$('#wrk-list').empty();
		for (var pos in table) {
			addWorker(table[pos].key, table[pos]);
		}
		
	};
	
	changeworkerstate = function(worker_ref, newstate) {
		$.ajax({url: url_changeworkerstate, type: "POST", async: false, data:{"worker_ref": worker_ref, "newstate": newstate}, success: refresh});
	};

	changeworkercyclicperiod = function(worker_ref, period) {
		$.ajax({url: url_changeworkercyclicperiod, type: "POST", async: false, data:{"worker_ref": worker_ref, "period": period}, success: refresh});
	};

	createSimpleKey = function(key) {
		return key.substring(7,16);
	};

	selectOnlyThisStatus = function(selectedstatus, selectedtaskjoblist) {
		var taskjobsortedlist = [];
		var current;
		for (var key in selectedtaskjoblist) {
			current = selectedtaskjoblist[key];
			if (current.status == selectedstatus) {
				current.key = key;
				current.simplekey = createSimpleKey(key);
				taskjobsortedlist[key] = current;
			}
		}
		return taskjobsortedlist;
	};

	refresh = function() {
		ajaxUpdateLists(url_getworkers, function(rawdata) {
			processingtasks = rawdata.processingtasks;

			var mustfullrefreshworkers = false;
			for (var key in rawdata.workers) {
				if (workerlist[key] == null) {
					mustfullrefreshworkers = true;
					break;
				}
			}
			if (mustfullrefreshworkers === false) {
				for (var key in workerlist) {
					if (rawdata.workers[key] == null) {
						mustfullrefreshworkers = true;
						break;
					}
				}
			}

			workerlist = rawdata.workers;
			
			if (mustfullrefreshworkers) {
				createfullworkertable();
			} else {
				for (var key in workerlist) {
					updateWorker(key, workerlist[key]);
				}
			}
			
			updateExcuseTextIfEmpty();
		});
	};

	updateWorkerStatusButton = function(key, worker) {
		if (enable_update === false) {
			return;
		}

		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrbtnstatus';
		var current_state = $(jqelement).data('currentstate');
		var content = ''; 
		var addaction = false;
		
		if (worker.statuschange == 'ENABLED') {
			if (current_state != 'setENABLED') {
				$(jqelement).data('currentstate', 'setENABLED');
				$(jqelement).html('<button class="btn btn-info btn-mini disabled" disabled="disabled">' + i18n('queue.worker.starting') + '</button>');
			}
		} else if (worker.statuschange == 'DISABLED') {
			if (current_state != 'setDISABLED') {
				$(jqelement).data('currentstate', 'setDISABLED');
				$(jqelement).html('<button class="btn btn-warning btn-mini disabled" disabled="disabled">' + i18n('queue.worker.stopping') + '</button>');
			}
		} else {
			switch (worker.status) {
				case "PROCESSING":
				case "WAITING":
				if (current_state != 'DISABLED') {
					$(jqelement).data('currentstate', 'DISABLED');
					$(jqelement).data('emkey', key);
					$(jqelement).html('<button class="btn btn-danger btn-mini"><i class="icon-stop icon-white"></i> ' + i18n('queue.worker.stopped') + '</button>');
					addaction = true;
				}
				break;
				case "STOPPED":
				case "PENDING_STOP":
				case "PENDING_CANCEL_TASK":
				if (current_state != 'ENABLED') {
					$(jqelement).data('currentstate', 'ENABLED');
					$(jqelement).data('emkey', key);
					$(jqelement).html('<button class="btn btn-success btn-mini"><i class="icon-play icon-white"></i> ' + i18n('queue.worker.started') + '</button>');
					addaction = true;
				}
				break;
			}
		}
		
		if (addaction) {
			$(jqelement + ' button').click(function() {
				var current_state = $(jqelement).data('currentstate');
				var key = $(jqelement).data('emkey');
				$.ajax({url: url_changeworkerstate, type: "POST", async: false, data:{"worker_ref": key, "newstate": current_state}, success: refresh});
			});
		}
		
	};

	updateWorkerStatus = function(worker) {
		switch (worker.status) {
			case "PROCESSING":
			return '<span class="label label-warning">' + i18n("PROCESSING") + '</span>';
			case "WAITING":
			return '<span class="label label-info">' + i18n("WAITING") + '</span>';
			case "STOPPED":
			return '<span class="badge badge-inverse">' + i18n("STOPPED") + '</span>';
			case "PENDING_STOP":
			return '<span class="label label-important">' + i18n("PENDING_STOP") + '</span>';
			case "PENDING_CANCEL_TASK":
			return '<span class="label label-warning">' + i18n("PENDING_CANCEL_TASK") + '</span>';
		}
		return worker.status;
	};

	updateCyclicWorkerCountdown = function(worker) {
		if (worker.status == 'PROCESSING') {
			if (worker.countdown_to_process > 300) {
				return '<span class="label label-info">Dans ' + worker.countdown_to_process + ' sec</span>';
			} else if (worker.countdown_to_process > 20) {
				return '<span class="label label-warning">Dans ' + worker.countdown_to_process + ' sec</span>';
			} else {
				return '<span class="label label-important">Dans ' + worker.countdown_to_process + ' sec</span>';
			}
		} else {
			return updateWorkerStatus(worker);
		}
	};
	
	addActionsForUpdateCyclicWorkerPeriod = function(key, worker) {
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrchngcyclprd';
		
		$(jqelement + ' .btnshowperiodchange').click(function() {
			if ($(jqelement + ' .input-append').css("display") == "none") {
				$(jqelement + ' .input-append').css("display", "inline");
			} else {
				$(jqelement + ' .input-append').css("display", "none");
			}
		});
		
		$(jqelement + ' .btnvalidnewperiod').click(function() {
			var period = $(jqelement + ' input').val();
			$.ajax({url: url_changeworkercyclicperiod, type: "POST", async: false, data:{"worker_ref": key, "period": period}, success: refresh});
		});
	};
	
	updateCyclicWorkerPeriod = function(key, worker) {
		if (worker.time_to_sleep === 0) {
			return;
		}
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrchngcyclprd';
		
		if (worker.changecyclicperiod) {
			$(jqelement + ' span.label').html(i18n('queue.worker.newperiod', worker.changecyclicperiod));
			$(jqelement + ' .btnshowperiodchange').css("display", "none");
		} else {
			$(jqelement + ' span.label').html(i18n('queue.worker.period', worker.time_to_sleep));
			$(jqelement + ' input').val(worker.time_to_sleep);
			$(jqelement + ' .btnshowperiodchange').css("display", "inline");
		}
	};
	
	addWorker = function(key, worker){
		var simplekey = createSimpleKey(key);
		var content = "";

		content = content + '<div class="row-fluid';
		if (worker.cyclic) {
			content = content + ' wkrcyclic';
		}
		content = content + '" id="wkr-' + simplekey + '" data-emkey="' + key + '">';
		
		content = content + '<div class="span4">';
		if (worker.cyclic) {
			content = content + '<button class="btn btn-mini disabled" disabled="disabled" type="button"><i class="icon-repeat"></i></button> ';
		} else {
			content = content + '<button class="btn btn-mini" data-target="#wkrfullview-' + simplekey + '" data-toggle="collapse" type="button"><i class="icon-chevron-down"></i></button> ';
		}
		
		content = content + '<strong class="wkrworkerlname">' + worker.long_worker_name + '</strong> on ';
		content = content + '<span class="wkrhostname label label-inverse">' + worker.hostname + '/' + worker.instancename + '</span>';
		content = content + '</div>';
		
		if (worker.cyclic) {
			content = content + '<div class="span1">';
			content = content + '<span class="wkrcountdownpro">' + updateCyclicWorkerCountdown(worker) + '</span> ';
			content = content + '</div>';
			content = content + '<div class="span7">';
			content = content + '<span class="wkrbtnstatus"></span> ';
			if (worker.time_to_sleep > 0) {
				content = content + '<span class="wkrchngcyclprd">';
				content = content + '<span class="label"></span> ';
				if (enable_update) {
					content = content + '<button class="btn btn-mini btnshowperiodchange" data-toggle="button" type="button">' + i18n('queue.worker.edit') + '</button> ';
					content = content + '<div class="input-append" style="display:none">';
					content = content + '<input type="number" class="span2"/>';
					content = content + '<button class="btn btnvalidnewperiod btn-warning" type="button">' + i18n('queue.worker.validate') + '</button>';
					content = content + '</div>';
				}
				content = content + '</span>';
			}
			
			content = content + '</div></div>';
		} else {
			content = content + '<div class="span1">';
			content = content + '<span class="wkrstatus">' + updateWorkerStatus(worker) + '</span> ';
			content = content + '</div>';
			content = content + '<div class="span7">';
			content = content + '<span class="wkrbtnstatus"></span> ';
			if (processingtasks[worker.job]) {
				content = content + '<span class="wkrjob">' + i18n('queue.worker.jobworking') + ' <strong>' +  processingtasks[worker.job].name + '</strong></span>';
			} else {
				content = content + '<span class="wkrjob"></span>';
			}
			content = content + '</div></div>';
			
			content = content + '<div id="wkrfullview-' + simplekey + '" class="collapse"><strong>' + i18n('queue.worker.managedprofiles') + '</strong><ul style="list-style-type:disc">';
			for (var pos in worker.managed_profiles) {
				content = content + '<li>' + worker.managed_profiles[pos].category + ': ' + worker.managed_profiles[pos].name + '</li>';
			}
			content = content + '</ul></div>';
		}
		
		$('#wrk-list').append(content);
		
		updateWorkerStatusButton(key, worker);
		
		if (worker.cyclic) {
			if (worker.time_to_sleep > 0) {
				updateCyclicWorkerPeriod(key, worker);
				addActionsForUpdateCyclicWorkerPeriod(key, worker);
			}
		}
	};
	
	updateWorker = function(key, worker){
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey;
		var jqelement_full = '#wkrfullview-' + simplekey;
		
		$(jqelement + ' .wkrworkerlname').html(worker.long_worker_name);
		$(jqelement + ' .wkrhostname').html(worker.hostname + '/' + worker.instancename);
				
		if (worker.cyclic) {
			$(jqelement + ' .wkrcountdownpro').html(updateCyclicWorkerCountdown(worker));
		} else {
			$(jqelement + ' .wkrstatus').html(updateWorkerStatus(worker));
			if (processingtasks[worker.job]) {
				$(jqelement + ' .wkrjob').html(i18n('queue.worker.jobworking') + ' <strong>' +  processingtasks[worker.job].name);
			} else {
				$(jqelement + ' .wkrjob').html('');
			}
			
			$(jqelement_full + ' ul').empty();
			var content = '';
			for (var pos in worker.managed_profiles) {
				content = content + '<li>' + worker.managed_profiles[pos].category + ': ' + worker.managed_profiles[pos].name + '</li>';
			}
			$(jqelement_full + ' ul').append(content);
		}
		
		updateWorkerStatusButton(key, worker);
		
		if (worker.cyclic & (worker.time_to_sleep > 0)) {
			updateCyclicWorkerPeriod(key, worker);
		}
	};

}