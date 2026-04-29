package com.example.springboot.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

/**
 * Spring Statemachine 配置类
 * 定义订单状态机的所有状态、事件和转换规则
 *
 * 状态转换流程：
 * PENDING_PAYMENT -> PAID -> PREPARING -> SHIPPED -> DELIVERED
 *
 * 允许的取消操作：
 * PENDING_PAYMENT -> CANCELLED -> REFUNDED
 * PAID -> CANCELLED -> REFUNDED
 *
 */
@Configuration
@EnableStateMachine
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    /**
     * 配置状态机的监听器
     */
    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws Exception {
        config
            .withConfiguration()
                .autoStartup(true)
                .listener(stateMachineListener());
    }

    /**
     * 配置所有可能的状态
     */
    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states
            .withStates()
                .initial(OrderState.PENDING_PAYMENT)
                .states(EnumSet.allOf(OrderState.class))
                .end(OrderState.DELIVERED)
                .end(OrderState.CANCELLED)
                .end(OrderState.REFUNDED);
    }

    /**
     * 配置状态转换
     * 定义在哪些事件下，从哪个状态转换到哪个状态
     */
    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
            // 待支付 -> 已支付
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.PAID)
                .event(OrderEvent.PAY)
            .and()
            // 已支付 -> 准备中
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.PREPARING)
                .event(OrderEvent.PREPARE)
            .and()
            // 准备中 -> 已发货
            .withExternal()
                .source(OrderState.PREPARING)
                .target(OrderState.SHIPPED)
                .event(OrderEvent.SHIP)
            .and()
            // 已发货 -> 已签收
            .withExternal()
                .source(OrderState.SHIPPED)
                .target(OrderState.DELIVERED)
                .event(OrderEvent.DELIVER)
            .and()
            // 待支付 -> 已取消
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.CANCELLED)
                .event(OrderEvent.CANCEL)
            .and()
            // 已支付 -> 已取消
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.CANCELLED)
                .event(OrderEvent.CANCEL)
            .and()
            // 准备中 -> 已取消
            .withExternal()
                .source(OrderState.PREPARING)
                .target(OrderState.CANCELLED)
                .event(OrderEvent.CANCEL)
            .and()
            // 已取消 -> 已退款
            .withExternal()
                .source(OrderState.CANCELLED)
                .target(OrderState.REFUNDED)
                .event(OrderEvent.REFUND)
            .and()
            // 已支付 -> 已退款（直接退款）
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.REFUNDED)
                .event(OrderEvent.REFUND)
            .and()
            // 任何状态 -> 待支付（恢复操作）
            .withInternal()
                .source(OrderState.PENDING_PAYMENT)
                .event(OrderEvent.RECOVER)
            .and()
            .withInternal()
                .source(OrderState.PAID)
                .event(OrderEvent.RECOVER)
            .and()
            .withInternal()
                .source(OrderState.PREPARING)
                .event(OrderEvent.RECOVER)
            .and()
            .withInternal()
                .source(OrderState.SHIPPED)
                .event(OrderEvent.RECOVER);
    }

    /**
     * 创建状态机监听器
     * 用于监听状态转换事件
     */
    @Bean
    public StateMachineListener<OrderState, OrderEvent> stateMachineListener() {
        return new StateMachineListenerAdapter<OrderState, OrderEvent>() {
            @Override
            public void stateChanged(State<OrderState, OrderEvent> from, State<OrderState, OrderEvent> to) {
                String fromState = from != null ? from.getId().toString() : "NONE";
                String toState = to != null ? to.getId().toString() : "NONE";
                System.out.println("状态转换: " + fromState + " -> " + toState);
            }

            @Override
            public void stateEntered(State<OrderState, OrderEvent> state) {
                System.out.println("进入状态: " + state.getId());
            }

            @Override
            public void stateExited(State<OrderState, OrderEvent> state) {
                System.out.println("退出状态: " + state.getId());
            }

            @Override
            public void eventNotAccepted(org.springframework.statemachine.event.StateMachineEvent event) {
                System.out.println("事件被拒绝: " + event.getMessage());
            }
        };
    }
}
