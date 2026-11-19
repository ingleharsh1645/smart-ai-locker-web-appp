package com.smartlocker.service;

import com.smartlocker.dto.LockerDtos.*; import com.smartlocker.entity.*; import com.smartlocker.exception.ApiException; import com.smartlocker.repository.*;
import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service
public class LockerService {
    private final StudentRepository students; private final LockerItemRepository items; private final TimetableEntryRepository timetable; private final LockerActivityRepository activities; private final UserAccountRepository users;
    public LockerService(StudentRepository students,LockerItemRepository items,TimetableEntryRepository timetable,LockerActivityRepository activities,UserAccountRepository users){this.students=students;this.items=items;this.timetable=timetable;this.activities=activities;this.users=users;}
    @Transactional(readOnly=true)
    public Student studentFromAuthentication(Authentication auth){ return students.findByRollNumber(auth.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Student not resolved")); }
    @Transactional(readOnly=true)
    public Student studentByIdForParent(Authentication auth){ if(auth==null||auth.getName()==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"Authentication required"); UserAccount account=auth.getName().contains("@")?users.findByEmail(auth.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Parent account not found")):users.findByMobile(auth.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Parent account not found")); Student student=account.getStudent(); student.getName(); return student; }
    @Transactional(readOnly=true)
    public SmartChecklist checklist(Student student){ DayOfWeek day=LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek(); var timetableRows=timetable.findByDayOfWeekOrderByIdAsc(day).stream().map(e->new TimetableItem(e.getPeriod(),e.getSubject(),e.isHomeworkRequired())).toList(); Set<String> homeworkSubjects=homeworkSubjects(day); var checklist=items.findByStudentId(student.getId()).stream().map(i->{boolean required=i.isRequiredToday()||homeworkSubjects.contains(i.getSubject().trim().toLowerCase(Locale.ROOT)); return new ChecklistItem(i.getId(),i.getName(),i.getCategory(),i.getSubject(),required,required?"TAKE_HOME":"LEAVE_IN_LOCKER");}).toList(); return new SmartChecklist(timetableRows,checklist,day); }
    @Transactional
    public ChecklistItem addItem(Student student,LockerItemRequest request){
        LockerItem item=LockerItem.builder()
                .student(student)
                .name(request.name().trim())
                .category(normalizeCategory(request.category()))
                .subject(request.subject().trim())
                .requiredToday(request.requiredToday())
                .build();
        items.save(item);
        return toChecklistItem(item,LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek());
    }
    @Transactional
    public ChecklistItem updateItem(Student student,Long itemId,LockerItemRequest request){
        LockerItem item=items.findById(itemId)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Locker item not found"));
        if(!item.getStudent().getId().equals(student.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN,"Locker item belongs to another student");
        item.setName(request.name().trim());
        item.setCategory(normalizeCategory(request.category()));
        item.setSubject(request.subject().trim());
        item.setRequiredToday(request.requiredToday());
        return toChecklistItem(item,LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek());
    }
    @Transactional
    public ActivityLog markActivity(Student student,ActivityRequest r){ LockerItem item=items.findById(r.itemId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Locker item not found")); if(!item.getStudent().getId().equals(student.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"Locker item belongs to another student"); LockerActivity a=LockerActivity.builder().student(student).lockerItem(item).action(r.action()).timestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))).build(); activities.save(a); return new ActivityLog(a.getId(),item.getId(),item.getName(),a.getAction(),a.getTimestamp()); }
    @Transactional(readOnly=true)
    public List<ActivityLog> studentActivityToday(Student student){
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        return activities.findByStudentIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(
                        student.getId(),today.atStartOfDay(),today.plusDays(1).atStartOfDay())
                .stream().map(a->new ActivityLog(a.getId(),a.getLockerItem().getId(),a.getLockerItem().getName(),a.getAction(),a.getTimestamp()))
                .toList();
    }
    @Transactional(readOnly=true)
    public ParentMonitor monitor(Student student){return monitor(student,LocalDate.now(ZoneId.of("Asia/Kolkata")));}
    @Transactional(readOnly=true)
    public ParentMonitor monitor(Student student,LocalDate day){
        if(day.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) throw new ApiException(HttpStatus.BAD_REQUEST,"Cannot view locker activity for a future date");
        LocalDateTime start=day.atStartOfDay();
        LocalDateTime end=day.plusDays(1).atStartOfDay();
        List<LockerActivity> dayActivities=activities.findByStudentIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(student.getId(),start,end);
        var logs=dayActivities.stream().map(a->new ActivityLog(a.getId(),a.getLockerItem().getId(),a.getLockerItem().getName(),a.getAction(),a.getTimestamp())).toList();
        Map<Long,LockerActivity.ActivityAction> latestActionByItem=new HashMap<>();
        dayActivities.forEach(a->latestActionByItem.putIfAbsent(a.getLockerItem().getId(),a.getAction()));
        Set<String> homeworkForDay=homeworkSubjects(day.getDayOfWeek());
        var missed=items.findByStudentId(student.getId()).stream()
                .filter(i->i.isRequiredToday()||homeworkForDay.contains(i.getSubject().trim().toLowerCase(Locale.ROOT)))
                .filter(i->latestActionByItem.get(i.getId())!=LockerActivity.ActivityAction.TAKEN_FROM_LOCKER)
                .map(LockerItem::getName).toList();
        return new ParentMonitor(student.getName(),student.getRollNumber(),day,logs,missed,missed.isEmpty());
    }
    private Set<String> homeworkSubjects(DayOfWeek day){return timetable.findByDayOfWeek(day).stream().filter(TimetableEntry::isHomeworkRequired).map(e->e.getSubject().trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());}
    private ChecklistItem toChecklistItem(LockerItem item,DayOfWeek day){
        boolean required=item.isRequiredToday()||homeworkSubjects(day).contains(item.getSubject().trim().toLowerCase(Locale.ROOT));
        return new ChecklistItem(item.getId(),item.getName(),item.getCategory(),item.getSubject(),required,required?"TAKE_HOME":"LEAVE_IN_LOCKER");
    }
    private String normalizeCategory(String category){return switch(category.trim().toUpperCase(Locale.ROOT)){case "BOOK"->"Book";case "NOTEBOOK"->"Notebook";default->"Other";};}
}
