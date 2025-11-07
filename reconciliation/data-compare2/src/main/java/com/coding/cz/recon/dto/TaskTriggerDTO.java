package com.coding.cz.recon.dto;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

//import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskTriggerDTO {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    private LocalDate t1Date; // 仅批量任务需要（T-1日期）
}