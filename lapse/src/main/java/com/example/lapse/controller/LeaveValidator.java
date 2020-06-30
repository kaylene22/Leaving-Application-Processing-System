package com.example.lapse.controller;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.example.lapse.domain.LeaveApplication;
import com.example.lapse.repo.LeaveApplicationRepo;
import com.example.lapse.service.LeaveApplicationService;
import com.example.lapse.service.LeaveApplicationServiceImpl;

public class LeaveValidator implements Validator {
	
	  @Autowired
	  private LeaveApplicationService laservice;
	  
	  @Autowired
	  public void setLeaveApplicationService(LeaveApplicationServiceImpl laserviceImpl) {
	    this.laservice = laserviceImpl;
	  }
	  @Autowired
	  LeaveApplicationRepo laRepo;

	  @Override
	  public boolean supports(Class<?> clazz) {
	    return LeaveApplication.class.equals(clazz);
	  }
	  

	  @Override
	  public void validate(Object target, Errors errors) {
	    LeaveApplication application = (LeaveApplication) target;
	    Calendar calStart = dateToCalendar(application.getStartDate());
	    Calendar calEnd = dateToCalendar(application.getEndDate());

//	    1. Applied startdate <= endDate (min 1 day)
	    boolean status = startDateBeforeEndDate(calStart, calEnd);
	    if (status == false) { 
			errors.rejectValue("dates", "Start Date is after End Date");
	    }

	    
//	    2. (Jaye) 
	    //Check if end date of previous transaction is not after current startdate ("Start date can not be in between other application date"
//

	    
	    
//	    3.Retrieve number of days in between start and end date (inclusive of start and end)
		float daysBetween = ChronoUnit.DAYS.between(calStart.toInstant(), calEnd.toInstant()) + 1;
		if(daysBetween <= 14) {
			daysBetween = removeWeekends(calStart, calEnd);
		}
		
	    
	    //Jayes part
	    List<LeaveApplication> lalist = laservice.findApplicationByStaffId(application.getId());
	      
	    if(application.getEndDate().before(application.getStartDate()))
	    {
	      errors.rejectValue("endDate", "Start date can not be later than end date");
	    }
	    
	    for (Iterator<LeaveApplication> iterator = lalist.iterator(); iterator.hasNext();) {
	        LeaveApplication application2 = (LeaveApplication) iterator.next();
	      if (application2.getEndDate().after(application.getStartDate())) {
	          errors.rejectValue("startDate", "Start date can not inbetween other application date");
	        }
	    }
	    
	    
	    //5. Check balance 
	    if ((application.getLeaveType().getEntitlement() - laRepo.getSumOfLeavesAppliedByStaff(application.getStaff().getId(), application.getLeaveType().getId()) - daysBetween) < 0) {
	          errors.rejectValue("endDate", "Not enough leave balance "+application.getLeaveType().getLeaveType());
	    };    
	    
	  }
	  
	  
	  
	  
	  
	  //convert Date format to Calendar format
	  public  Calendar dateToCalendar(Date date){ 
		  Calendar cal = Calendar.getInstance();
		  cal.setTime(date);
		  return cal;
		}
	  
	  //Check if start date is <= end date
		public boolean startDateBeforeEndDate(Calendar start, Calendar end) {
            if(start.getTimeInMillis() <= end.getTimeInMillis()) {
    			return true;
            }
            else return false;
		};
		
		//Remove weekends when days applied  <=14
		public float removeWeekends(Calendar start, Calendar end) {
			float daysWithoutWeekends = 0;
			do {
				  if (start.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
						  start.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {;
				  daysWithoutWeekends++; }
				  start.add(Calendar.DAY_OF_MONTH, 1);
				  } 
				while (start.getTimeInMillis() <= end.getTimeInMillis());
					return daysWithoutWeekends;
			
		}
	  
}