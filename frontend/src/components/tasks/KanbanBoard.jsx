'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { DndContext, DragOverlay, closestCorners, KeyboardSensor, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, arrayMove, sortableKeyboardCoordinates, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { MoreHorizontal, Calendar, User } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { priorityBadgeClass, priorityLabel, statusDotClass } from '@/lib/task-ui';

const TaskCard = ({ task, onEdit, onDelete, onView }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const getInitials = (name) => {
    return name
      ?.split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase() || 'U';
  };

  return (
    <Card
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={() => onView?.(task)}
      className="cursor-grab border-border/80 active:cursor-grabbing hover:border-primary/40 hover:-translate-y-0.5 transition-[border-color,transform] duration-200"
    >
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <CardTitle className="text-sm font-medium leading-tight">
            {task.title}
          </CardTitle>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              {/* Stop the click from also bubbling up to the card's own
                  onClick (which opens the detail modal) — the "..." menu
                  needs to open on its own, not underneath another modal. */}
              <Button variant="ghost" size="icon" className="h-6 w-6" onClick={(e) => e.stopPropagation()}>
                <MoreHorizontal className="h-3 w-3" />
              </Button>
            </DropdownMenuTrigger>
            {/* Radix portals this content, but React's synthetic events still
                bubble through the component tree to the Card's onClick — this
                stops Edit/Delete clicks from also opening the detail modal. */}
            <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
              <DropdownMenuItem onClick={() => onEdit(task)}>
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onDelete(task.id)} className="text-destructive">
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        {task.description && (
          <p className="text-xs text-muted-foreground mb-3 line-clamp-2">
            {task.description}
          </p>
        )}
        
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {task.priority && (
              <span className={priorityBadgeClass(task.priority)}>
                {priorityLabel(task.priority)}
              </span>
            )}
            {task.dueDate && (
              <div className="flex items-center text-xs text-muted-foreground">
                <Calendar className="h-3 w-3 mr-1" />
                {new Date(task.dueDate).toLocaleDateString()}
              </div>
            )}
          </div>
          
          {task.assigneeEmail && (
            <div className="flex items-center text-xs text-muted-foreground">
              <User className="h-3 w-3 mr-1" />
              <Avatar className="h-5 w-5">
                <AvatarFallback className="text-xs">
                  {getInitials(task.assigneeEmail)}
                </AvatarFallback>
              </Avatar>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

const Column = ({ title, tasks, status, onEdit, onDelete, onView, index = 0 }) => {
  const taskIds = tasks.map(task => task.id);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay: index * 0.06 }}
      className="flex h-full flex-col rounded-lg border border-border bg-secondary/30 p-4"
    >
      <div className="mb-4 flex items-center justify-between">
        <h3 className="flex items-center gap-2 font-mono text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          <span className={statusDotClass(status)} />
          {title}
        </h3>
        <Badge variant="secondary" className="font-mono text-xs">
          {tasks.length}
        </Badge>
      </div>
      
      <SortableContext items={taskIds} strategy={verticalListSortingStrategy}>
        <div className="flex-1 space-y-3 overflow-y-auto">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onEdit={onEdit}
              onDelete={onDelete}
              onView={onView}
            />
          ))}
        </div>
      </SortableContext>
    </motion.div>
  );
};

export default function KanbanBoard({ tasks, onTaskUpdate, onTaskEdit, onTaskDelete, onTaskView }) {
  const [activeTask, setActiveTask] = useState(null);
  
  const sensors = useSensors(
    // Without an activation constraint, PointerSensor claims every pointerdown
    // on a sortable card as a potential drag from pixel zero — which also
    // swallows the native click event, so clicking a card to open its detail
    // view (onView, wired in the Card below) silently did nothing. Requiring
    // 8px of movement before a drag is recognized is dnd-kit's own documented
    // fix for "click and drag need to coexist on the same element".
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const columns = [
    { id: 'TODO', title: 'To Do', status: 'TODO' },
    { id: 'IN_PROGRESS', title: 'In Progress', status: 'IN_PROGRESS' },
    { id: 'PENDING', title: 'Pending', status: 'PENDING' },
    { id: 'DONE', title: 'Done', status: 'DONE' },
  ];

  const getTasksByStatus = (status) => {
    return tasks.filter(task => task.status === status);
  };

  const handleDragStart = (event) => {
    const { active } = event;
    const task = tasks.find(t => t.id === active.id);
    setActiveTask(task);
  };

  const handleDragEnd = (event) => {
    const { active, over } = event;
    setActiveTask(null);

    if (!over) return;

    const activeTask = tasks.find(t => t.id === active.id);
    if (!activeTask) return;

    // Determine the new status based on the drop zone
    let newStatus = activeTask.status;
    
    // Check if dropped on a different column
    const overColumn = columns.find(col => col.id === over.id);
    if (overColumn) {
      newStatus = overColumn.status;
    } else {
      // Check if dropped on a task in a different column
      const overTask = tasks.find(t => t.id === over.id);
      if (overTask) {
        newStatus = overTask.status;
      }
    }

    // Update task status if it changed
    if (newStatus !== activeTask.status) {
      onTaskUpdate(activeTask.id, {
        title: activeTask.title,
        description: activeTask.description,
        status: newStatus,
        priority: activeTask.priority,
        assigneeEmail: activeTask.assigneeEmail || null,
        dueDate: activeTask.dueDate || null,
      });
    }
  };

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 h-[600px]">
        {columns.map((column, index) => (
          <SortableContext key={column.id} items={[column.id]}>
            <Column
              title={column.title}
              tasks={getTasksByStatus(column.status)}
              status={column.status}
              index={index}
              onEdit={onTaskEdit}
              onDelete={onTaskDelete}
              onView={onTaskView}
            />
          </SortableContext>
        ))}
      </div>

      <DragOverlay>
        {activeTask ? (
          <TaskCard
            task={activeTask}
            onEdit={() => {}}
            onDelete={() => {}}
          />
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}

