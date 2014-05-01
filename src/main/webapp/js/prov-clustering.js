var currentProvJSON = ""; //document which is collapsed and expanded
var clusteredProvJSON = ""; //clustered document without expanding or collapsing nodes
var sourceProvJSON = ""; // original document without any clustering



function clusterUsingSlider(){
	var numCommunities = $("#num-clusters").val();
	$('#num-clusters-form').val(numCommunities);
	cluster(numCommunities);
}

function clusterUsingForm(){
	var numCommunities = $("#num-clusters-form").val();
	$('#num-clusters').val(numCommunities);
	cluster(numCommunities);
}

function cluster(numCommunities){
	startLoading();
	var url = "api/cluster/" + numCommunities;
	$.post(
			url, 
			{ document: sourceProvJSON }, 
			function(data){
				clusteredProvJSON = data;
				currentProvJSON = data;
				convertToSVG(data);
			});
	return false;
}

function convertToSVG(data){
	$.ajax({
		type: 'POST',
		url: 'api/convert/svg',
		data: {
			'document': data
		},
		success: function(msg){
			$("#svgcontainer").hide().html(msg).fadeIn("fast");
			stopLoading();
		},
		error: function(jqXHR, textStatus, errorThrown) {
			error_message("Could not convert document to SVG: " + errorThrown);
		}
	});
}

function collapse(clusterID){
	var clusterLabel = prompt("Please enter a name for this cluster","");
	if(clusterLabel == null){
		clusterLabel == "";
	}
	startLoading();
	var url = 'api/collapse/' + clusterID;
	$.ajax({
		type: 'POST',
		url: url,
		data: {
			'clustered-document': currentProvJSON,
			'cluster-label': clusterLabel
		},
		success: function(data){
			currentProvJSON = data;
			convertToSVG(data);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			error_message("Could not collapse cluster: " + errorThrown);
		}
	});
}

function expand(clusterID){
	startLoading();
	var url = 'api/expand/' + clusterID;
	$.ajax({
		type: 'POST',
		url: url,
		data: {
			'clustered-document': currentProvJSON,
			'original-clustered-document': clusteredProvJSON
		},
		success: function(msg){
			currentProvJSON = msg;
			convertToSVG(msg);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			error_message("Could not expand cluster: " + errorThrown);
		}
	});
}
function getDocumentViewFromProvStore(view){
	$('.view-button').removeClass('active');
	if(view == 'data'){
		$('#data-button').addClass('active');
	}else if( view == 'process'){
		$('#process-button').addClass('active');
	}else if (view == 'responsibility'){
		$('#responsibility-button').addClass('active');
	}
	
	startLoading();
	var documentId = $('#document-id-input').val();
	var api = new $.provStoreApi({username: 'ajk4g11-2', key: '7eba64280d544d4201f5c31d31d600c0ab78a9f0'});
	api.request(
		'documents/'+documentId+'/views/'+view+'.json',
		'',
		'GET',
		function(data){
			json_str = JSON.stringify(data);
			sourceProvJSON = json_str;
			get_document_detail(documentId);
			convertToSVG(json_str);
		},
		function(error){
			error_message("Could not retrieve document view from ProvStore:" + error);
		}
	);
}
function getDocumentFromProvStore(){
	$('.view-button').removeClass('active');
	$('#all-button').addClass('active');
	startLoading();
	$('#svgparent').show();
	$('#clustering-options').show();
	var documentId = $('#document-id-input').val();
	var api = new $.provStoreApi({username: 'ajk4g11-2', key: '7eba64280d544d4201f5c31d31d600c0ab78a9f0'});
	api.getDocumentBody(
		documentId, 
		'json', 
		function(data){
			json_str = JSON.stringify(data);
			sourceProvJSON = json_str;
			get_document_detail(documentId);
			convertToSVG(json_str);
		}, 
		function(error){
			error_message("Could not retrieve document from ProvStore: " + error);
		}
	);
}

function saveDocumentToProvStore(){
	console.log("saving");
	var title = $('#save-document-title').val();
	var api = new $.provStoreApi({username: 'ajk4g11-2', key: '7eba64280d544d4201f5c31d31d600c0ab78a9f0'});
	console.log(JSON.parse(currentProvJSON));
	api.submitDocument(
			title,
			JSON.parse(currentProvJSON),
			"false",
			function(id){
				status_message("Saved to ProvStore successfully with document id " + id);
				add_version(id);
			},
			function(error){
				error_message("Could not save document to ProvStore: " + error);
			}
	);
}

function add_version(id){
	var url = '<a href="https://provenance.ecs.soton.ac.uk/store/documents/'+id+'">' + id + '</a><br>';
	$('#versions').append(url);
}
function get_document_detail(id){
	var api = new $.provStoreApi({username: 'ajk4g11-2', key: '7eba64280d544d4201f5c31d31d600c0ab78a9f0'});
	api.getDocument(
		      id,
			  function(data){
			    set_document_detail(data);
			  },
			  function(error){
				  error_message("Error whilst getting document details: " + error);
			  }
			);
}
function set_document_detail(data){
	var content = "<p><b>Document name:</b> " + data.document_name + "<br>";
	content += "<b>ID:</b> " + data.id + "<br>";
	content += "<b>Owner:</b> " + data.owner + "</p>";
	$('#document-info-content').html(content);
	$('#document-info').show();
}
function status_message(message){
	stopLoading();
	$('#status-success').show();
	$('#status-success-message').html("<strong>Success: </strong>" + message);
}

function error_message(message){
	stopLoading();
	$('#status-error').show();
	$('#status-error-message').html("<strong>Error: </strong>" + message);
}

function startLoading(){
	$('#loading').show();
}
function stopLoading(){
	$('#loading').hide();
}
function initJQueryZoom(){
	var $section = $('#svgparent');
    var $panzoom = $section.find('#svgcontainer').panzoom();
    $panzoom.parent().on('mousewheel.focal', function( e ) {
        e.preventDefault();
        var delta = e.delta || e.originalEvent.wheelDelta;
        var zoomOut = delta ? delta < 0 : e.originalEvent.deltaY > 0;
        $panzoom.panzoom('zoom', zoomOut, {
          increment: 0.4,
          minScale: 0.5,
          maxScale: 10,
          focal: e
        });
    });
}

function load_modal(){
	$('#load-modal').modal('show');
	
}
function save_modal(){
	$('#save-modal').modal('show');
}

$(function() {
	$('#loading').hide();
	$('#document-info').hide();
	$('#status-success').hide();
	$('#status-error').hide();
	$('#clustering-options').hide();
	initJQueryZoom(); //setup zooming plugin	
});