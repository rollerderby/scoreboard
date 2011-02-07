/**
 * jQuery xml plugin
 * Converts XML node(s) to string 
 *
 * Copyright (c) 2009 Radim Svoboda
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 *
 * @author 	Radim Svoboda, user Zzzzzz
 * @version 1.0.0
 */
 
   
/**
 * Converts XML node(s) to string using web-browser features.
 * Similar to .html() with HTML nodes 
 * This method is READ-ONLY.
 *  
 * @param all set to TRUE (1,"all",etc.) process all elements,
 * otherwise process content of the first matched element 
 *  
 * @return string obtained from XML node(s)  
 */         
jQuery.fn.xml = function(all) {
      
  //result to return
  var s = "";
  
   //Anything to process ?
   if( this.length )
  
    //"object" with nodes to convert to string  
   (
      ( ( typeof all != 'undefined' ) && all ) ?
      //all the nodes 
      this 
      :
      //content of the first matched element 
      jQuery(this[0]).contents()
    )
   //convert node(s) to string  
   .each(function(){
    s += window.ActiveXObject ?//==  IE browser ?
       //for IE
	     this.xml
	     :
	     //for other browsers
	     (new XMLSerializer()).serializeToString(this)
	     ;
  }); 
    
    
  return	s;		
  
  };
                     