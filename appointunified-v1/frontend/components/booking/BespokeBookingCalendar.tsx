'use client'

import React, { useMemo } from 'react'
import { 
  addMonths, subMonths, format, startOfMonth, endOfMonth, 
  eachDayOfInterval, isSameMonth, isSameDay, isToday, isBefore, startOfDay 
} from 'date-fns'
import { ChevronLeft, ChevronRight, Loader2, ArrowRight } from 'lucide-react'
import { AvailableSlot } from '@/types'
import { cn, formatTimeOnly } from '@/lib/utils'

interface BespokeBookingCalendarProps {
  currentMonth: Date
  selectedDate: Date | null
  selectedSlot: AvailableSlot | null
  slotsForSelectedDate: AvailableSlot[]
  isLoadingSlots: boolean
  onMonthChange: (date: Date) => void
  onDateSelect: (date: Date) => void
  onSlotSelect: (slot: AvailableSlot) => void
  onConfirm: () => void  // To proceed to the next step
}

export function BespokeBookingCalendar({
  currentMonth,
  selectedDate,
  selectedSlot,
  slotsForSelectedDate,
  isLoadingSlots,
  onMonthChange,
  onDateSelect,
  onSlotSelect,
  onConfirm
}: BespokeBookingCalendarProps) {

  // Generate calendar days
  const daysInMonth = useMemo(() => {
    const start = startOfMonth(currentMonth)
    const end = endOfMonth(currentMonth)
    const days = eachDayOfInterval({ start, end })
    
    // Add padding for start of month (e.g., if month starts on Wednesday, add Sun, Mon, Tue)
    const prefixDays = Array(start.getDay()).fill(null)
    return [...prefixDays, ...days]
  }, [currentMonth])

  const nextMonth = () => onMonthChange(addMonths(currentMonth, 1))
  const prevMonth = () => onMonthChange(subMonths(currentMonth, 1))
  
  const today = startOfDay(new Date())

  return (
    <div className="flex flex-col md:flex-row gap-8 bg-white rounded-2xl md:p-8 shadow-sm border border-slate-100 min-h-[450px]">
      
      {/* Left Pane - Calendar Widget */}
      <div className={cn("flex-1 transition-all duration-300", selectedDate ? 'md:max-w-[60%]' : 'max-w-full lg:max-w-[70%] mx-auto')}>
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-bold text-slate-800">{format(currentMonth, 'MMMM yyyy')}</h3>
          <div className="flex items-center gap-2">
            <button 
              onClick={prevMonth} 
              disabled={isBefore(currentMonth, startOfMonth(today))}
              className="p-2 rounded-full hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft size={20} className="text-slate-600" />
            </button>
            <button 
              onClick={nextMonth} 
              className="p-2 rounded-full hover:bg-slate-100 transition-colors"
            >
              <ChevronRight size={20} className="text-slate-600" />
            </button>
          </div>
        </div>

        {/* Days of week header */}
        <div className="grid grid-cols-7 gap-1 mb-2 text-center text-[11px] font-semibold text-slate-400 tracking-wider uppercase">
          {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
            <div key={day} className="py-2">{day}</div>
          ))}
        </div>

        {/* Days grid */}
        <div className="grid grid-cols-7 gap-y-2 gap-x-1 sm:gap-x-2">
          {daysInMonth.map((date, i) => {
            if (!date) return <div key={`empty-${i}`} />
            
            const isPast = isBefore(date, today)
            const isSelected = selectedDate && isSameDay(date, selectedDate)
            const isCurrentToday = isToday(date)

            return (
               <button
                  key={date.toISOString()}
                  onClick={() => !isPast && onDateSelect(date)}
                  disabled={isPast}
                  className={cn(
                    "relative flex items-center justify-center w-full aspect-square rounded-full text-sm font-medium transition-all mx-auto max-w-[44px]",
                    isPast ? "text-slate-300 cursor-not-allowed bg-transparent" :
                    isSelected ? "bg-brand-600 text-white shadow-lg shadow-brand-500/30 font-bold transform scale-105" :
                    "hover:bg-brand-50 hover:text-brand-700 text-slate-700",
                    isCurrentToday && !isSelected && "text-brand-600 font-bold bg-brand-50/50"
                  )}
               >
                 {format(date, 'd')}
                 {isCurrentToday && !isSelected && (
                   <div className="absolute bottom-1 w-1 h-1 rounded-full bg-brand-600" />
                 )}
               </button>
            )
          })}
        </div>
      </div>

      {/* Right Pane - Time Slots (slides in dynamically via CSS transitions) */}
      {selectedDate && (
        <div className="flex-1 w-full md:w-64 border-t md:border-t-0 md:border-l border-slate-100 pt-6 md:pt-0 md:pl-8 animate-in fade-in slide-in-from-right-4 duration-500 ease-out">
          <div className="flex flex-col h-full">
            <h3 className="text-[15px] font-semibold mb-5 text-slate-900 border-b border-slate-100 pb-3 flex items-center">
               <span className="bg-brand-50 text-brand-600 text-xs px-2 py-1.5 rounded-lg mr-2 font-bold uppercase tracking-wider">
                 {format(selectedDate, 'EEE')}
               </span>
               <span className="text-slate-600 font-medium">
                 {format(selectedDate, 'MMMM d')}
               </span>
            </h3>

            {isLoadingSlots ? (
              <div className="flex-1 flex flex-col items-center justify-center py-12">
                <Loader2 size={24} className="animate-spin text-brand-500 mb-2" />
                <p className="text-xs text-slate-400 font-medium tracking-wide">Finding available times...</p>
              </div>
            ) : slotsForSelectedDate.length === 0 ? (
              <div className="flex-1 flex flex-col items-center justify-center py-12 px-4 text-center">
                <div className="w-12 h-12 bg-slate-50 text-slate-400 rounded-full flex items-center justify-center mb-3">
                  <div className="w-2 h-2 rounded-full bg-slate-300" />
                </div>
                <p className="text-slate-600 font-medium mb-1">No times available</p>
                <p className="text-xs text-slate-400">Please choose a different date.</p>
              </div>
            ) : (
              <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-2.5 max-h-[320px]">
                {slotsForSelectedDate.map((slot) => {
                  const isSelected = selectedSlot?.startTime === slot.startTime

                  return (
                    <div key={slot.startTime} className="flex gap-2 w-full animate-in fade-in zoom-in-95 duration-300 fill-mode-both" style={{ animationDelay: `${Math.random() * 150}ms` }}>
                      <button
                        onClick={() => slot.available && onSlotSelect(slot)}
                        disabled={!slot.available}
                        className={cn(
                          "py-3 rounded-xl border text-sm font-medium transition-all group overflow-hidden relative",
                          isSelected 
                            ? "w-1/2 bg-slate-800 border-slate-800 text-white" 
                            : "w-full hover:border-brand-600 hover:text-brand-700 bg-white border-slate-200 text-slate-700 shadow-[0_1px_2px_rgba(0,0,0,0.02)]",
                          !slot.available && "opacity-50 cursor-not-allowed bg-slate-50 border-slate-100 hover:border-slate-100 line-through text-slate-400"
                        )}
                      >
                         <span className="relative z-10">{formatTimeOnly(slot.startTime)}</span>
                         {!isSelected && slot.available && (
                           <div className="absolute inset-0 bg-brand-50/0 group-hover:bg-brand-50/50 transition-colors z-0" />
                         )}
                      </button>
                      
                      {isSelected && (
                         <button 
                            onClick={onConfirm}
                            className="flex-1 bg-brand-600 text-white rounded-xl font-semibold text-sm hover:bg-brand-700 transition-colors flex items-center justify-center gap-1.5 shadow-md shadow-brand-500/20 animate-in slide-in-from-right-4 fade-in"
                         >
                            Next <ArrowRight size={14} />
                         </button>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </div>
      )}
      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar { width: 4px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
      `}</style>
    </div>
  )
}
